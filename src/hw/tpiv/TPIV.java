package hw.tpiv;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.plugin.Duplicator;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class TPIV {
	
	ImagePlus imp = null;
	ImagePlus imp_roi = null;
	//ImagePlus sep_imp[][]; ->これだとスレッドセーフでかすかに動かないため(ごく一部で違う値が出てしまう)
    ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ImagePlus>> sep_imp;
	ImagePlus new_imp = null;
	
	int width;
	int height;
	int slice;
	
	String title;

	Roi roi = null;
	int roi_width = 1;
	int roi_height = 1;
	
	// // PIV 基本設定 ////
	int windowWidth = 16;
	int windowHeight = 16;
	int overlap = 50; // 50%
	int extendValue = 5;
	int colorMethod = 0;
	double maxLengthLimitValue = 8.0;
	double limitValue = 0.8;
	int subPixelValue = 10;
	
	int unit_width;
	int unit_height;
	int x_point;
	int y_point;

	Color[] colors;

	Overlay[] overlay; //t
	Overlay empty_overlay;
	
	
	// / PIV shifr array ///

	//int[][][][] piv_shift_array = null; // [t][x][y][x_position,y_position]
	//int[][][][] shift_array = null;

	double[][][][] piv_shift_array = null;
	double[][][][] shift_array = null;

	double[][][][] calcValueArray = null; //[t][x][y][length, radian]
	double maxLength = 0.0;

	public TPIV(ImagePlus img){
		imp = img;
		roi = img.getRoi();
		width = img.getWidth();
		height = img.getHeight();
		slice = img.getStackSize();
		title = img.getTitle();
		
		//imp_roi = img.duplicate();
		imp_roi = new Duplicator().run(img); //1.52n対策
		roi_width = imp_roi.getWidth();
		roi_height = imp_roi.getHeight();
		
		unit_width = (int) (windowWidth * ((100.0 - overlap) / 100.0));
		unit_height = (int) (windowHeight * ((100.0 - overlap) / 100.0));
		x_point = (roi_width / unit_width) - 1;
		y_point = (roi_height / unit_height) - 1;


		colors = new Color[8];
		colors[0] = new Color(100, 0, 100);
		colors[1] = new Color(0, 0, 255);
		colors[2] = new Color(100, 150, 255);
		colors[3] = new Color(0, 255, 200);
		colors[4]= new Color(0, 255, 0);
		colors[5] = new Color(255, 255, 0);
		colors[6] = new Color(255, 150, 0);
		colors[7] = new Color(255, 0, 0);


		makeOverlay();

	}

	public int getYpoint(){
		return y_point;
	}
	
	public int getXpoint(){
		return x_point;
	}
	
	public void setRoi(Roi r){
		roi = r;
	}
	
	public void setWindowWidth(int ww){
		windowWidth = ww;
		unit_width = (int) (windowWidth * ((100.0 - overlap) / 100.0));
		x_point = (roi_width / unit_width) - 1;
	}
	
	public void setWindowHeight(int wh){
		windowHeight = wh;
		unit_height = (int) (windowHeight * ((100.0 - overlap) / 100.0));
		y_point = (roi_height / unit_height) - 1;
	}
	
	public void setOverlap(int o){
		overlap = o;
		
		unit_width = (int) (windowWidth * ((100.0 - overlap) / 100.0));
		x_point = (roi_width / unit_width) - 1;

		unit_height = (int) (windowHeight * ((100.0 - overlap) / 100.0));
		y_point = (roi_height / unit_height) - 1;

	}
	
	public void setExtendValue(int ev){
		extendValue = ev;
	}

	public void setMaxLengthLimitValue(double l){
		maxLengthLimitValue = l;
	}

	public void setColorMethod(int v){
		colorMethod = v;
	}

	public void setLimitValue(double v) {
		limitValue = v;
	}

	public void setSubPixelValue(int v){
        subPixelValue = v;
    }

	public void makeOverlay() {
		overlay = new Overlay[slice];
		for (int ct = 0; ct < slice; ct++) {
			overlay[ct] = new Overlay();
		}				

	}
	
	public ImagePlus makePIVimage(boolean withOimg, boolean overWrite, boolean checkMedian, boolean angleColor){

		makeSepImp();
		getShiftArray();

		if(checkMedian == true){
			correctByMedian();
		}

		calcTwoPositionValues();


		if (withOimg == true) {

			imp.killRoi();
			new_imp = imp.duplicate();

			if (new_imp.getCompositeMode() != IJ.COLOR) {
				StackConverter sc = new StackConverter(new_imp);
				sc.convertToRGB();
				IJ.showStatus("");
			}

			imp.setRoi(roi);
			new_imp.setRoi(roi);

		} else {

			//new_imp = imp.duplicate();
			new_imp = new Duplicator().run(imp); //1.52n対策
			new_imp.setStack(ImageStack.create(width, height, slice, 24));
		}

		ConcurrentHashMap<Integer, Integer> countMap = new ConcurrentHashMap<>();
		IntStream t_stream = IntStream.range(1, slice);
		t_stream.parallel().forEach(t -> {
                countMap.put(t, 1);
                this.progress("Creating arrow... ", countMap.size(), slice);
                makeArrow(t, overWrite, angleColor);
	    });
		
		
		return new_imp;		
		
	}

	private void makeArrow(int ct, boolean overWrite, boolean angleColor){
		//IJ.showStatus("makeArrow");
		//IJ.showProgress(ct, slice);

		double roi_x = 0;
		double roi_y = 0;
		if (roi != null) {
			roi_x = roi.getXBase();
			roi_y = roi.getYBase();
		}

		
		ImageProcessor imp_p = new_imp.getStack().getProcessor(ct + 1);
		imp_p.setColor(Color.WHITE);
		
		for (int y = 0; y < y_point; y++) {


			for (int x = 0; x < x_point; x++) {

				// 倍率と、矢印のサイズによって表示される矢印の方向がおかしくなる、、、(あまりにも長さが小さい値の場合なるようだ)

				double ox = piv_shift_array[0][x][y][0] + roi_x;
				double oy = piv_shift_array[0][x][y][1] + roi_y;

				// double extend_value = 4; //shift量の最大値がwindow
				// sizeの 1/4のため。
				// double extend_value = 3;
				// //矢印の表示が正しくなるのが3pixelのため。最小値が3になるように。
				double extend_value = extendValue;

				double ex = ox + (piv_shift_array[ct][x][y][0] * extend_value);
				double ey = oy + (piv_shift_array[ct][x][y][1] * extend_value);


				//double l = calcLength(ox, oy, ex, ey);
				//double r = calcRadian(ox, oy, ex, ey);

				double l = calcValueArray[ct][x][y][0];
				double r = calcValueArray[ct][x][y][1];

				Color c;
				if(angleColor == true){
				    c = compartRoiColorWithAngle(l, limitValue, r);
                }else{
					// colorMethodによる色分けの方法の変化

					if(colorMethod == 1) {
						c = compartRoiColor(l, maxLengthLimitValue);

					}else if(colorMethod == 2) {
						c = compartRoiColor2(l, limitValue);

					}else{//default : Hue表示
						c = compartRoiColor3(l, limitValue);
					}
                }
				imp_p.setColor(c);

				if ((l == 0)) {
					Roi b_roi = new Roi(ox, oy, 0, 0);

					if (overWrite == true) {
						imp_p.draw(b_roi);
					} else {
						overlay[ct].add(b_roi);
					}

				} else {
					Arrow a_roi = new Arrow(ox, oy, ex, ey);
					Arrow.setColor(Toolbar.getForegroundColor());

					a_roi.setStrokeWidth(compartStorkeWidth(l));
					a_roi.setHeadSize(compartHeadSize(l)); // windowWidthが7以下は考えない

					if (overWrite == true) {
						imp_p.draw(a_roi);
					} else {
						overlay[ct].add(a_roi);
					}
				}

			}
		}
		
	}
	
	public void calcTwoPositionValues(){

		calcValueArray = new double[slice][x_point][y_point][2]; //x,y,t,[length, radian]に変更

		for (int ct = 0; ct < slice; ct++) {
			for (int y = 0; y < y_point; y++) {
				for (int x = 0; x < x_point; x++) {

					if (ct == 0) {
						calcValueArray[ct][x][y][0] = 0.0;
						calcValueArray[ct][x][y][1] = 0.0;

					} else {

						double ox = piv_shift_array[0][x][y][0];
						double oy = piv_shift_array[0][x][y][1];

						double ex = ox + ((piv_shift_array[ct][x][y][0]));
						double ey = oy + ((piv_shift_array[ct][x][y][1]));

						double l = this.calcLength(ox, oy, ex, ey);
						double r = this.calcRadian(ox, oy, ex, ey);

						calcValueArray[ct][x][y][0] = l;
						calcValueArray[ct][x][y][1] = r;

						if(maxLength < l){
							maxLength = l;
						}
					}

				}
			}
		}

	}





	public Overlay[] getOverlay(){
		return overlay;
	}

	private void getShiftArray() {
		piv_shift_array = new double[slice][x_point][y_point][2]; //x,y,t,zahyoに変更

		// / shift計算 ///
		
		IntStream t_stream = IntStream.range(0, slice);
        ConcurrentHashMap<Integer, Integer> shiftCount = new ConcurrentHashMap<>();
		t_stream.parallel().forEach(ct -> {
            shiftCount.put(ct, 1);
            this.progress("Calculating shift value... ", shiftCount.size(), slice);
			secondProcess(ct);
		});

		//TEST FFT, 2021.11.12
		System.out.println("FFT TEST");
		CalcFFT test = new CalcFFT(piv_shift_array);
		Point p = new Point(0,0);
		float[] fx = test.getPowerSpectrumX(p);
		float[] fy = test.getPowerSpectrumY(p);
		System.out.println("XPower");
		for(int a = 0; a < fx.length; a++) {
			System.out.println(fx[a]);
		}
		System.out.println("YPower");
		for(int a = 0; a < fy.length; a++) {
			System.out.println(fy[a]);
		}
	}

	public void progress(String s, int current, int all){
	    IJ.showProgress(current / all);
        IJ.showStatus(s + current + " / " + all);
    }

	
	public double[][][][] getShiftedArray(){
		return piv_shift_array;
	}
	
	private void secondProcess(int ct){
	    for(int cy = 0; cy < y_point; cy++){
			for(int cx = 0; cx < x_point; cx++){
				if (ct == 0) {
					int p_x = (unit_width) * cx + (unit_width);
					int p_y = (unit_height) * cy + (unit_height);

					double buff_array[] = { p_x, p_y };
					piv_shift_array[ct][cx][cy] = buff_array;

				} else {
				    ImagePlus buff_imp = sep_imp.get(cx).get(cy);
					//ImagePlus buff_imp = sep_imp[cx][cy];
					/*
					CalPOCtpiv poc = new CalPOCtpiv(windowWidth,windowHeight);
					ImageProcessor ip1 = buff_imp.getStack().getProcessor(ct);
					ImageProcessor ip2 = buff_imp.getStack().getProcessor(ct + 1);
					piv_shift_array[ct][cx][cy] = poc.getPosition2(ip1,ip2);
					*/

					LineProjection lineProjection = new LineProjection(buff_imp);
					piv_shift_array[ct][cx][cy] = lineProjection.getPosition(ct, ct+1, subPixelValue);
					//System.out.println(piv_shift_array[ct][cx][cy][0]);
				}
			}
		}
	}
	

	public void correctByMedian() {

		shift_array = new double[slice][x_point][y_point][2];

		IntStream t_stream = IntStream.range(0, slice);
		//t_stream.parallel().forEach(ct -> medianProcess(ct));
		t_stream.parallel().forEach(ct -> medianProcess2(ct));

		piv_shift_array = shift_array;
	}


	private void medianProcess(int ct){

		for (int y = 0; y < y_point; y++) {
			for (int x = 0; x < x_point; x++) {

				if (ct == 0) {
					int p_x = (unit_width) * x + (unit_width);
					int p_y = (unit_height) * y + (unit_height);

					double buff_array[] = { p_x, p_y };
					shift_array[ct][x][y] = buff_array;
				} else {

					double[] buff_array_x = new double[9];
					double[] buff_array_y = new double[9];



					int num = 0;
					for (int fx = -1; fx < 2; fx++) {
						for (int fy = -1; fy < 2; fy++) {

							if (((x + fx) >= 0) && ((y + fy >= 0))) { // どっちもプラスの場合(左、上にはみ出さない場合)
								if (((x + fx) < x_point)&& ((y + fy < y_point))) { // どっちも超えない場合(右、下ににはみ出さない場合)

									buff_array_x[num] = piv_shift_array[ct][x + fx][y + fy][0];
									buff_array_y[num] = piv_shift_array[ct][x + fx][y + fy][1];

								} else if (((x + fx) >= x_point)&& ((y + fy < y_point))) { // xだけはみ出る場合
									buff_array_x[num] = piv_shift_array[ct][x_point - 1][y][0];
									buff_array_y[num] = piv_shift_array[ct][x_point - 1][y][1];

								} else if (((x + fx) < x_point)&& ((y + fy >= y_point))) { // yだけはみ出る場合
									buff_array_x[num] = piv_shift_array[ct][x][y_point - 1][0];
									buff_array_y[num] = piv_shift_array[ct][x][y_point - 1][1];
								}

							} else if (((x + fx) >= 0)&& ((y + fy < 0))) { // yだけマイナスの場合
								buff_array_x[num] = piv_shift_array[ct][x][0][0];
								buff_array_y[num] = piv_shift_array[ct][x][0][1];

							} else if (((x + fx) < 0)&& ((y + fy >= 0))) { // xだけマイナスの場合
								buff_array_x[num] = piv_shift_array[ct][0][y][0];
								buff_array_y[num] = piv_shift_array[ct][0][y][1];
							}

							num++;
						}
					}

					Arrays.sort(buff_array_x);
					Arrays.sort(buff_array_y);

					double buff_array[] = { buff_array_x[3],buff_array_y[3] };
					shift_array[ct][x][y] = buff_array;

				}

			}
		}

	}


	private void medianProcess2(int ct){

		for (int y = 0; y < y_point; y++) {
			for (int x = 0; x < x_point; x++) {

				if (ct == 0) {
					int p_x = (unit_width) * x + (unit_width);
					int p_y = (unit_height) * y + (unit_height);

					double buff_array[] = { p_x, p_y };
					shift_array[ct][x][y] = buff_array;
				} else {

					ArrayList<Double> buffArrayX = new ArrayList<>();
					ArrayList<Double> buffArrayY = new ArrayList<>();


					for (int fx = -1; fx < 2; fx++) {
						for (int fy = -1; fy < 2; fy++) {

							if (((x + fx) >= 0) && ((y + fy >= 0))) { // どっちもプラスの場合(左、上にはみ出さない場合)
								if (((x + fx) < x_point)&& ((y + fy < y_point))) { // どっちも超えない場合(右、下ににはみ出さない場合)

									buffArrayX.add(piv_shift_array[ct][x + fx][y + fy][0]);
									buffArrayY.add(piv_shift_array[ct][x + fx][y + fy][1]);

								} else if (((x + fx) >= x_point)&& ((y + fy < y_point))) { // xだけはみ出る場合
									buffArrayX.add(piv_shift_array[ct][x_point - 1][y][0]);
									buffArrayY.add(piv_shift_array[ct][x_point - 1][y][1]);

								} else if (((x + fx) < x_point)&& ((y + fy >= y_point))) { // yだけはみ出る場合
									buffArrayX.add(piv_shift_array[ct][x][y_point - 1][0]);
									buffArrayY.add(piv_shift_array[ct][x][y_point - 1][1]);
								}

							} else if (((x + fx) >= 0)&& ((y + fy < 0))) { // yだけマイナスの場合
								buffArrayX.add(piv_shift_array[ct][x][0][0]);
								buffArrayY.add(piv_shift_array[ct][x][0][1]);

							} else if (((x + fx) < 0)&& ((y + fy >= 0))) { // xだけマイナスの場合
								buffArrayX.add(piv_shift_array[ct][0][y][0]);
								buffArrayY.add(piv_shift_array[ct][0][y][1]);
							}

						}
					}

					buffArrayX.sort(Comparator.naturalOrder());
					buffArrayY.sort(Comparator.naturalOrder());

					double remainder = buffArrayX.size() % 2.0;
					int medianX = buffArrayX.size() / 2;
					int medianY = buffArrayY.size() / 2;

					double buff_array[] = {buffArrayX.get(medianX), buffArrayY.get(medianY)};

					if(remainder == 0){ //偶数
						buff_array[0] = (buff_array[0] + buffArrayX.get(medianX - 1)) / 2.0;
						buff_array[1] = (buff_array[1] + buffArrayY.get(medianY - 1)) / 2.0;
					}
					shift_array[ct][x][y] = buff_array;

				}

			}
		}

	}


	
	
	public void makeSepImp() {
		IJ.showStatus("makeSepImage");
		//sep_imp = new ImagePlus[x_point][y_point];
        sep_imp = new ConcurrentHashMap<>();

        for(int x = 0; x < x_point; x++){
            ConcurrentHashMap<Integer, ImagePlus> buff = new ConcurrentHashMap<>();
            sep_imp.put(x, buff);
        }

		IntStream x_stream = IntStream.range(0, x_point);
		x_stream.forEach(x -> sepProcess(x)); //ここの並列が微妙な数値の違いの原因！
		
	}
	private void sepProcess(int xp){
		IJ.showProgress(xp, x_point);
		
		for(int yp = 0; yp < y_point; yp++){
			//sep_imp[xp][yp] = sep_of_roi(xp, yp);
            sep_imp.get(xp).put(yp, sep_of_roi(xp, yp));
		}
	}
	
	
	public ImagePlus sep_of_roi(int x, int y){
		int window_s_point_y = unit_height * y;
		int window_s_point_x = unit_width * x;
		Roi windowRoi = new Roi(window_s_point_x, window_s_point_y, windowWidth, windowHeight);
		imp_roi.setRoi(windowRoi);
		//ImagePlus return_img = imp_roi.duplicate();
		ImagePlus return_img = new Duplicator().run(imp_roi); //1.52n対策
		return return_img;
	}
	
	public int makeExtendValue() { // 現在4倍固定のため未使用
		int result = 0;

		int bigger_value = windowWidth;
		if (windowHeight > windowWidth) {
			bigger_value = windowHeight;
		}

		return result;
	}
	
	public double calcLength(double ox, double oy, double ex, double ey) {
		double result = 0.0;

		result = Math.sqrt(Math.pow((ex - ox), 2) + Math.pow((ey - oy), 2));

		if (Double.isNaN(result)) {
			result = 0.0;
		}

		return result;
	}

	public double calcRadian(double ox, double oy, double ex, double ey){
        //double radian = Math.atan2(ey - oy, ex - ox); //角度が計算できない場合はどうする？
		double radian = Math.atan2((-ey) - (-oy), ex - ox); //モニター座標から普通座標へ変換しておく？

	    return -radian;
    }


	public double compartStorkeWidth(double l) {
		double result = 2.0;

		/*
		 * result = Math.rint(l * 2);
		 * 
		 * if(result < 2){ result = 2.0; }
		 */

		return result;
	}

	public double compartHeadSize(double l) {
		double result = 3.0;
		/*
		 * result = l ; if(l < 2){ result = 2.0; }else if(l > 7){ result = 7.0;
		 * }
		 */
		return result;
	}

	public Color compartRoiColor(double l, double maxL) { // 長さによって色を変える, 20210831追記 maxLを最大値として8分割する。
		/*
		 * 色の順番、配色をどうするか？8色、シュートカラー風？
		 */
		Color result = colors[colors.length-1];
		double unitValue = maxL / colors.length;

		for(int i = 0; i < colors.length; i++){
			if(l < (unitValue * (i+1))){
				result = colors[i];
				break;
			}
		}
		return result;

	}

	public Color compartRoiColor2(double l, double limit) { // maxLengthにたいしてのRatioで変える

		//Color result = Toolbar.getForegroundColor();
		Color result = colors[colors.length-1];

		double unitValue = 1.0 / colors.length ;
		double value = l / (maxLength * limit);

		for(int i = 0; i < colors.length; i++){
			if(value < (unitValue * (i+1))){
				result = colors[i];
				break;
			}
		}
		return result;

	}

	public Color compartRoiColor3(double l, double limit){ //色が背景色と見分けづらい場合が多い -> 20210902 反転で解決
		Color result;
		double hue = l / (maxLength * limit);
		if(hue > 1.0){
			hue = 1.0;
		}

		double brightness = hue * 1.5;
		double saturation = 1.0;

		if(brightness > 1.0){
			brightness = 1.0;
		}
		result = Color.getHSBColor((float)(1.0 - hue), (float)saturation, (float)brightness);
		return result;
	}



	public Color compartRoiColorWithAngle(double l, double limit, double r){
        Color result;
		double lValue = Math.round((l / maxLength / limit));

        double hue = r / (Math.PI * 2);
		//double brightness = l / 10.0;
        double brightness = lValue;
        double saturation = 1.0;

        if(brightness > 1){
            brightness = 1.0;
        }
        result = Color.getHSBColor((float)hue, (float)saturation, (float)brightness);
        return result;
    }



	public void showColorBox(int w, int h){ //compartRoiColorで使用される色の見本を表示する
		ImageProcessor buff = new ColorProcessor(w, 1);
		double maxlabel = ((double)Math.round((maxLength * limitValue) * 100)) / 100;
		double maxlabelActualValue = ((double)Math.round((maxLength * limitValue * imp.getCalibration().pixelWidth) * 100)) / 100;
		if(colorMethod == 1){ //Absoluteだけ別値
			maxlabel = ((double)Math.round((maxLengthLimitValue * 100))) / 100;
			maxlabelActualValue = ((double)Math.round((maxLengthLimitValue * imp.getCalibration().pixelWidth) * 100)) / 100;

		}

		if(colorMethod == 0) {
			for(int i = 0; i < w; i++){
				double hue = (i / (double)w);
				if(hue > 1.0){
					hue = 1.0;
				}
				double brightness = hue * 1.5;
				if(brightness > 1.0){
					brightness = 1.0;
				}

				Color c = Color.getHSBColor((float)(1.0 - hue), 1.0f, (float)brightness);
				buff.set(i, c.getRGB());
			}

		}else{
			int unit = w / colors.length;
			for (int i = 0; i < w; i++){
				int classification = i / unit; //切り捨て
				buff.set(i, colors[classification].getRGB());
			}

		}

		int margin = 15;
		ColorProcessor resized = (ColorProcessor) buff.resize(w, (h + (2 * margin))) ; //上下の余白5pixel分追加
		for(int x = 0; x < w; x++) {
			for (int y = 0; y < margin; y++) {
				resized.set(x, y, Color.WHITE.getRGB());
				resized.set(x, (margin + h + y), Color.WHITE.getRGB());
			}

		}
		TextRoi[] annotations = new TextRoi[4];
		annotations[0] = new TextRoi(0, 0, "0");
		annotations[1] = new TextRoi((w - TextRoi.getDefaultFontSize() -10), 0, String.valueOf(maxlabel));
		annotations[2] = new TextRoi(0, (margin + h), "0");
		annotations[3] = new TextRoi((w - TextRoi.getDefaultFontSize() -10), (margin + h), String.valueOf(maxlabelActualValue));


		resized.setColor(Color.BLACK);
		for(TextRoi textRoi: annotations){
			resized.draw(textRoi);
		}
		ImagePlus result = new ImagePlus();
		result.setProcessor("ColorBox", resized);
		result.show();
	}


	public void saveTSV(String dir, String save_file_name) {
		
		File f = new File(dir + save_file_name);

		FileWriter filewriter = null;
		try {
			filewriter = new FileWriter(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedWriter bw = new BufferedWriter(filewriter);

		PrintWriter pw = new PrintWriter(bw);

		String x_c;
		String y_c;
		String coordinate;

		for (int ct = 0; ct < slice; ct++) {
			pw.println("S:" + (ct + 1) + ";");
			for (int y = 0; y < y_point; y++) {
				for (int x = 0; x < x_point; x++) {

					if (ct == 0) {

						x_c = String.valueOf(piv_shift_array[ct][x][y][0]);
						y_c = String.valueOf(piv_shift_array[ct][x][y][1]);
						coordinate = x_c + "," + y_c;
					} else {
						x_c = String.valueOf(piv_shift_array[ct][x][y][0] + piv_shift_array[0][x][y][0]);
						y_c = String.valueOf(piv_shift_array[ct][x][y][1] + piv_shift_array[0][x][y][1]);
						coordinate = x_c + "," + y_c;
					}
					pw.print(coordinate);
					pw.print("\t");
				}
				pw.print("\n");
			}
		}

		pw.close();
	}


	public void saveValuesTSV(String dir, String save_file_name, int option) { //豊岡やよい様 要望 20200914

		File f = new File(dir + save_file_name);

		FileWriter filewriter = null;
		try {
			filewriter = new FileWriter(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		BufferedWriter bw = new BufferedWriter(filewriter);

		PrintWriter pw = new PrintWriter(bw);

		String x_c;
		String y_c;
		String coordinate;

		pw.println(x_point + " x " + y_point);
		pw.print("PositionID");
		for(int t = 0; t < slice; t++){
			pw.print("\t");
			pw.print("T" + t);
		}
		pw.print("\n");

		String value = "";
		for(int y = 0; y < y_point; y++){
			for(int x = 0; x < x_point; x++){
				pw.print(x + "-" + y);

				for(int ct = 0; ct < slice; ct++){

					if(option == 0) {
						if (ct == 0) {
							value = "0";
						} else {
							value = String.valueOf(calcValueArray[ct][x][y][0]);
						}

					}else if(option == 1){
						if(ct == 0) {
							value = "0";
						}else {
							value = String.valueOf(-calcValueArray[ct][x][y][1]); //符号が色表示用に逆になっている？
						}

					} else if (option == 2) {

						if (ct == 0) {
							x_c = String.valueOf(piv_shift_array[ct][x][y][0]);
							y_c = String.valueOf(piv_shift_array[ct][x][y][1]);
							coordinate = x_c + "," + y_c;
						} else {
							x_c = String.valueOf(piv_shift_array[ct][x][y][0] + piv_shift_array[0][x][y][0]);
							y_c = String.valueOf(piv_shift_array[ct][x][y][1] + piv_shift_array[0][x][y][1]);
							coordinate = x_c + "," + y_c;
						}

						value = coordinate;
					}

					pw.print("\t");
					pw.print(value);

				}
				pw.print("\n");
			}
		}


		pw.close();
	}

	
}