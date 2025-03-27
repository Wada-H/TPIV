package hw.tpiv;
import ij.*;
import ij.process.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

import static java.lang.Math.*;

/*
 * 20140630
 * ここまで位相限定相関法についていろいろ試したこと等を書いてみる
 * 2次元(画像）へのフーリエ変換はxy軸各行列にたいしておこなう。
 * 離散、fftは2のべき乗をとるひつようがある。
 * 逆変換においては順変換後の共役複素数をとったかたちで順変換、出た数値をNで割る
 * なぜか鏡像で戻る。。。
 * 同一画像においてのズレに関しては確かにそれっぽい鋭いピークが見られるが
 * timelapse画像においてはそれほぞ鋭いピークは現れないのかもしれない。
 * そのぼんやり具合の中心を見つけるような式があるようだが、、、、
 * それと、元画像のようにショットノイズがある場合はかなり相関なしとなってしまうのか
 * ピークらしいものが見られなくなる。ノイズに強いとあるので、私のプログラム、考え方のミスかもしれないが。
 * median(r=5)位をかけると多少ましになるようである。
 * これらの結果より、timelapse等で見ている画像がそれなりに変化してしまう場合は適応するのが難しいかもしれない。
 * ->当初考えていた、単純な相関係数を見た方がよいかもしれない。
 * x軸の1次元として相関をみてずらす。次にy軸の1次元として相関を見る、みたいな。
 * いちばん相関の高い位置を探す方法。
 */

/*
 * 20150520
 * CalPOCch 完全コピー
 * PIVに使用
 */


public class CalPOCtpiv {
	
	private int n;
	private int width;
	private int height;
	
	public CalPOCtpiv(int width, int height) {
		this.n = width * height;
		this.width = width;
		this.height = height;

	}

/* 20140630-POCタイプ中止	
	public int[] getPosition8(byte[] pixels1, byte[] pixels2){
		int[] position_array = {0,0};
		
		//byte[xy]からdouble[x][y]へ変換
		double[][] pixels1_d = new double[height][width] ;
		double[][] pixels2_d = new double[height][width] ;
		
		
		for (int y = 0; y < height ;y++ ){
			for(int x = 0; x < width; x++){
				int idx = (y * width) + x;
				pixels1_d[y][x] = pixels1[idx] & 0xff;
				pixels2_d[y][x] = pixels2[idx] & 0xff;
			}
		}

		//double[][] test = new double[height][width];
		//sortXtoY(pixels1_d,test);
		//sortXtoY(test,pixels1_d);
		
		//fft

		double[][] pixels1_i_data = new double[height][width];
		double[][] pixels2_i_data = new double[height][width];
		
		fft2d_to_phase(pixels1_d, pixels1_i_data, 1);
		fft2d_to_phase(pixels2_d, pixels2_i_data, 1);
		
		//積算
		double[][] multiplication_r_data = new double[height][width];
		double[][] multiplication_i_data = new double[height][width];

		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				multiplication_r_data[y][x] = (pixels1_d[y][x] * pixels2_d[y][x]);
				multiplication_i_data[y][x] = (pixels1_i_data[y][x] * (-pixels2_i_data[y][x]));
				//java.lang.System.out.println(pixels2_i_data[y][x]);
				//java.lang.System.out.println(-pixels2_i_data[y][x]);

				//multiplication_r_data[y][x] = (pixels1_d[y][x] * pixels1_d[y][x]);
				//multiplication_i_data[y][x] = (pixels1_i_data[y][x] * -pixels1_i_data[y][x]);
			}
		}
		//java.lang.System.out.println(multiplication_r_data[8][8]);
		
		//rfft
		rfft2d(multiplication_r_data, multiplication_i_data); //idfd使用、おそい！
		//rfft2d(pixels2_d, pixels2_i_data); //idfd使用、おそい！

		//java.lang.System.out.println(multiplication_r_data[8][8]);


		//直線化->multiplicationの時にするか？
		double[] l_data = new double[n];
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				int idx = (y * width) + x ;
				//l_data[idx] = multiplication_r_data[y][x] + multiplication_i_data[y][x];
				l_data[idx] = 20 * -log10((multiplication_r_data[y][x]*multiplication_r_data[y][x]) + (multiplication_i_data[y][x]*multiplication_i_data[y][x]));
				//java.lang.System.out.println(l_data[idx]);
				//l_data[idx] = log10(pixels1_d[y][x]*pixels1_d[y][x] + pixels1_i_data[y][x] * pixels1_i_data[y][x])*100;

			}
		}
		//tile化
		l_data = sort4tile(l_data,width, height);	


		//テスト表示用

		ImageProcessor temp_ip = new ByteProcessor(width, height);
		byte[] temp_ip_pixels = (byte[])temp_ip.getPixels();
		
		int[] nvalue = new int[n];

		for (int i = 0; i < n ;i++ ){
			nvalue[i] = (int)l_data[i];
			//nvalue[i] = (int)((y_fft1[i]/Math.abs(y_fft1[i])) * (y_fft2[i]/Math.abs(y_fft2[i])));
			//nvalue[i] = (int)((pixels1_d[i] / Math.abs(pixels1_d[i])) * (pixels2_d[i]/ Math.abs(pixels2_d[i])));
			//nvalue[i] = (int)(Math.abs(pixels1_d[i]));///Math.abs(pixels1_d[i]));
			//nvalue[i] = (int)(y_fft1[i] * y_fft2[i]);
			temp_ip_pixels[i] = (byte)nvalue[i];
			//java.lang.System.out.println(y_fft1[i]);

		}

		ImageStack New_Stack = new ImageStack(width,height);
	    New_Stack.addSlice("TEST_fft",temp_ip);
	    new ImagePlus("TEST_fft",New_Stack).show();
		
		return position_array;
	
	}
*///ここまで中止


	public int[] getTest(int n, int m){
		System.out.println("getTest");
		int[] test = new int[]{n,m};
		return test;
	}
	
	//ここから相関係数を見る方法に変更
	//public int[] getPosition8(byte[] pixels1, byte[] pixels2, int xOffset, int yOffset){
	public int[] getPosition(ImageProcessor ip1, ImageProcessor ip2, int xOffset, int yOffset){
	//getPosition8を変更
		System.out.println("getPosition");

		int[] position_array = {0,0};

		double[][] pixels1_d = new double[height][width] ;
		double[][] pixels2_d = new double[height][width] ;
		
		
		for (int y = 0; y < height ;y++ ){
			for(int x = 0; x < width; x++){
				pixels1_d[y][x] = (double)ip1.getPixel(x, y);
				pixels2_d[y][x] = (double)ip2.getPixel(x, y);;
			}
		}
		
		
		int shift_x = 0;
		int shift_y = 0;
		
		//画像全体を使わずに、ある行および列で行ってみる。とりあえず、真ん中で。->4点に変更してみる。それの中央値か平均値。->全部->512x512だと結構遅い

		double[][] temp_pixel_array1 = new double[height][width];
		temp_pixel_array1 = pixels1_d;
		//double[][] temp_pixel_array1 = new double[5][width];
		//temp_pixel_array1 = pixels1_d;
		//temp_pixel_array1[0] = pixels1_d[0];
		//temp_pixel_array1[1] = pixels1_d[height/4 * 1 -1];
		//temp_pixel_array1[2] = pixels1_d[height/4 * 2 -1];
		//temp_pixel_array1[3] = pixels1_d[height/4 * 3 -1];
		//temp_pixel_array1[4] = pixels1_d[height/4 * 4 -1];

		
		//y軸方向の相関
		int data_length = temp_pixel_array1.length;
		int[] ypos_array = new int[data_length];
		for(int x = 0; x < data_length; x++){  //xとしているがheigthの繰り返しすなわちy相当
			int ypos = 0;
			double ycoc = 0.0; //相関係数

			for(int y = 0; y < height; y++){
				double new_coc = 0;
				new_coc = getCOC(temp_pixel_array1[x],pixels2_d[y]);
				//java.lang.System.out.println(new_coc);
				if(new_coc > ycoc){
					ycoc = new_coc;
					ypos = y;
				}
			}
			//java.lang.System.out.println(ypos);
		
			ypos_array[x] = ypos - x;
		}
		Arrays.sort(ypos_array);
		
		//転置
		double[][] p1_xy = new double[width][height];
		double[][] p2_xy = new double[width][height];
		sortXtoY(pixels1_d, p1_xy);
		sortXtoY(pixels2_d, p2_xy);

		double[][] temp_pixel_array2 = new double[width][height];
		temp_pixel_array2 = p1_xy;
		//double[][] temp_pixel_array2 = new double[4][height];
		//temp_pixel_array2 = p1_xy;
		//temp_pixel_array2[0] = p1_xy[0];
		//temp_pixel_array2[1] = p1_xy[width/4 * 1 -1];
		//temp_pixel_array2[2] = p1_xy[width/4 * 2 -1];
		//temp_pixel_array2[3] = p1_xy[width/4 * 3 -1];
		//temp_pixel_array2[4] = p1_xy[width/4 * 4 -1];		

		//x軸方向の相関
		data_length = temp_pixel_array2.length;
		int[] xpos_array = new int[data_length];
		for(int y = 0; y < data_length; y++){
			int xpos = 0;
			double xcoc = 0.0;
			for(int x = 0; x < width; x++){
				double new_coc = 0;
				new_coc = getCOC(temp_pixel_array2[y],p2_xy[x]);
				if(new_coc > xcoc){
					xcoc = new_coc;
					xpos = x;
				}
			}
			xpos_array[y] = xpos - y;
			//java.lang.System.out.println(xpos_array[y]);
		}
		Arrays.sort(xpos_array);
		
		int median_position_y = (int)ceil(temp_pixel_array1.length/4*2-10); //はんぶんの10下のスコア使用->数点の平均値にするか？
		int median_position_x = (int)ceil(temp_pixel_array2.length/4*2+10);
		//java.lang.System.out.println(median_position_y);

		shift_y = (ypos_array[median_position_y + yOffset]); //中央値 + offset
		shift_x = (xpos_array[median_position_x + xOffset]); //中央値 + offset
		//shift_y = ave(ypos_array); //平均値 ->だめ、いまいち。
		//shift_x = ave(xpos_array); //平均値
		
		//System.out.println(shift_x + ","  + shift_y);

		position_array[0] = shift_x;
		position_array[1] = shift_y;
		return position_array;
	}

	public static double[] sortXYtoYX(double[] xy, int width, int height){
		int l = xy.length;
		double[] yx = new double[l] ;
		int i = 0;
		for (int x = 0; x < width ; x++){
			for(int y = 0; y < height ; y++){
				int idx = x + y*width;				
				yx[i] = xy[idx];
				i++;
				//java.lang.System.out.println(i);
			}
			
		}
		return yx;
	}

	public static void sortXtoY(double[][] data, double[][] c_data){
		int width = data.length;
		int height = data[0].length;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				c_data[y][x] = data[x][y];
			}
		}
	}

	public static void sortXtoY(byte[][] data, byte[][] c_data){
		int width = data.length;
		int height = data[0].length;
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				c_data[y][x] = data[x][y];
			}
		}
	}
	public static double[] sort4tile(double[] orignal_data, int width, int height){
		int length = width * height ;
		int wd = width / 2;
		int hd = height / 2;
		double[] tile_data = new double[length];
		for(int h = 0 ; h < hd ; h++){
			for(int w = 0 ; w < wd ; w++){
				int p1 = (width * (h + hd)) + (w + wd);
				int p2 = width * (h + hd) + w ;
				int p3 = width * h + (w + wd);
				int p4 = width * h + w;
				tile_data[p4] = orignal_data[p1];
				tile_data[p3] = orignal_data[p2];
				tile_data[p2] = orignal_data[p3];
				tile_data[p1] = orignal_data[p4];				
			}
		}
		 return tile_data;

	}

	public static void sort4tile(byte[] orignal_data, int width, int height){
		int length = width * height ;
		int wd = width / 2;
		int hd = height / 2;
		byte[] tile_data = new byte[length];
		for(int h = 0 ; h < hd ; h++){
			for(int w = 0 ; w < wd ; w++){
				int p1 = (width * (h + hd)) + (w + wd);
				int p2 = width * (h + hd) + w ;
				int p3 = width * h + (w + wd);
				int p4 = width * h + w;
				tile_data[p4] = orignal_data[p1];
				tile_data[p3] = orignal_data[p2];
				tile_data[p2] = orignal_data[p3];
				tile_data[p1] = orignal_data[p4];				
			}
		}
		orignal_data = tile_data;
	}
	

	public static void fft2d_to_phase(double[][] data, double[][] i_data, int option){
		//otion = 1 or 2 , 1:fft, 2:rfft
		int width = data[1].length;
		//int height = data.length;
		int height = data.length;

		double[][] fft_data = new double[width][height];
		double[][] temp_i_data = new double[width][height];
		
		//x軸方向にfft
		for(int i = 0; i < height; i++){		
			fft(data[i],i_data[i],1);
		}
		
		//xy入れ替え
		sortXtoY(data,fft_data);
		sortXtoY(i_data,temp_i_data);
		
		//y軸方向にfft
		for(int i = 0; i < width; i++){
			fft(fft_data[i],temp_i_data[i],1);
		}
		
		//元に戻す
		sortXtoY(fft_data,data);
		sortXtoY(temp_i_data,i_data);

///*		
		//位相算出（振幅で正規化）
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				double spectrum = Math.sqrt((data[y][x]*data[y][x]) + (i_data[y][x]*i_data[y][x]));			
				data[y][x] = data[y][x] / spectrum;
				i_data[y][x] = i_data[y][x] / spectrum;
				//java.lang.System.out.println(data[y][x]);

			}
		}
//*/		
	}
	


	//2d逆フーリエ
	public static void rfft2d(double[][] re, double[][] im){
		int width = re[1].length;
		int height = re.length;

		double[][] temp_re = new double[width][height];
		double[][] temp_im = new double[width][height];

		
		//x軸方向にfft
		for(int i = 0; i < height; i++){		
			fft(re[i],im[i],2);
		}

		//xy入れ替え
		sortXtoY(re,temp_re);
		sortXtoY(im,temp_im);		

		//y軸方向にfft
		for(int i = 0; i < width; i++){
			fft(temp_re[i],temp_im[i],2);
		}
		
		//元に戻す
		sortXtoY(temp_re,re);
		sortXtoY(temp_im,im);	
		


	}
	
	public static void fft(double[] real,double[] imaginary,int option){
		final int N = real.length;

		if(option == 2){ //逆変換用符号変換
			for(int i = 0 ; i < N; i++){

				imaginary[i] = -imaginary[i];

			}
		}
		
		double theta = -2*Math.PI/N;
	    for(int n = N,nh;  (nh = n>>1) >=1;  n = nh){
	        double cosTheta = Math.cos(theta);
	        double sinTheta = Math.sin(theta);

	        double cos = 1;//cos(mθ)はm=0から開始するので初期値は1
	        double sin = 0;//同様に初期値は0
	        for(int m=0;m < nh;m++){
	            for(int k = m;k < N;k+=n){
	                double
	                r,i,
	                Rm = real[k]-(r=real[k+nh]),
	                Im = imaginary[k]-(i=imaginary[k+nh]);
	                real[k] +=r;
	                imaginary[k] += i;
	                real[k+nh] = Rm*cos-Im*sin;
	                imaginary[k+nh] = Rm*sin+Im*cos;
	            }
	            //cos,sinを加法定理を用いて更新します。
	            double c = cos*cosTheta-sin*sinTheta;//cos(A+B)=cosAcosB-sinAsinB
	            double s = sin*cosTheta+cos*sinTheta;//sin(A+B)=sinAcosB+cosAsinB
	            cos = c;
	            sin = s;
	        }
	        theta *=2;
	    }

		if(option == 2){ //逆変換用Nで割る
			for(int i = 0 ; i < N; i++){
				real[i] = real[i] / N;
				imaginary[i] = imaginary[i]/N;
			}
		}

		
		//並べ替え
	    for (int j = 0,i=0,nh = N>>1,nh1 =nh+1; j < nh; j += 2) {
	        double tmpr,tmpi;
	        if (j < i) {
	            tmpr = real[j];
	            real[j] = real[i];
	            real[i] = tmpr;
	            tmpr = real[j + nh1];
	            real[j + nh1] = real[i + nh1];
	            real[i + nh1] = tmpr;
	            tmpi = imaginary[j];
	            imaginary[j] = imaginary[i];
	            imaginary[i] = tmpi;
	            tmpi = imaginary[j + nh1];
	            imaginary[j + nh1] = imaginary[i + nh1];
	            imaginary[i + nh1] = tmpi;
	        }
	        tmpr = real[j + nh];
	        real[j + nh] = real[i + 1];
	        real[i + 1] = tmpr;
	        tmpi = imaginary[j + nh];
	        imaginary[j + nh] = imaginary[i + 1];
	        imaginary[i + 1] = tmpi;
	        for (int k = nh >> 1; k > (i ^= k); k >>= 1);
	    }

	}

    public static double getCOC( double[] x,  double[] y ) { //COC(coefficient of correlation)
    	//r = 変数ｘと変数yの共分散 / 変数xの標準偏差 x 変数yの標準偏差 ->下記がそのような計算になっているか不明。

    	double r;
    	int n; //xとyの組数
        double  xt,yt,x2t,y2t,xyt,xh,yh,xs,ys;
        n = x.length; xt = 0; yt = 0; xyt = 0; x2t = 0; y2t = 0;
        double xsd,ysd;


        for( int  i=0; i<n; i++)  {
             xt += x[i];   yt += y[i]; //合計値
             x2t += x[i]*x[i];    y2t += y[i]*y[i]; //各分散用自乗
             xyt += x[i]*y[i]; //共分散用自乗
        }
        xh = xt/n; //平均値
        yh = yt/n;
        xsd=x2t/n-xh*xh; //分散
        ysd=y2t/n-yh*yh; 
        xs = Math.sqrt(xsd); //標準偏差
        ys = Math.sqrt(ysd);
        r = (xyt/n-xh*yh)/(xs*ys); //相関係数
     return r;
    }

    public static double getCOC2(double[] x, double[] y){ //おそい。。。
		double result;


		ArrayList<Double> xList = new ArrayList<>();
		ArrayList<Double> yList = new ArrayList<>();
		ArrayList<Double> xyList = new ArrayList<>();

		for(int i = 0; i < x.length; i++){
			xList.add(x[i]);
			yList.add(y[i]);
			xyList.add(x[i]*y[i]);
		}

		double aveX = xList.parallelStream().mapToDouble(d -> d).average().getAsDouble();
		double aveY = yList.parallelStream().mapToDouble(d -> d).average().getAsDouble();
		double sdX = Math.sqrt(xList.parallelStream().mapToDouble(d -> Math.pow((d - aveX) , 2)).average().getAsDouble());
		double sdY = Math.sqrt(yList.parallelStream().mapToDouble(d -> Math.pow((d - aveY) , 2)).average().getAsDouble());
		double covariance = xyList.parallelStream().mapToDouble(d -> d).average().getAsDouble() - (aveX * aveY);
		result = covariance / (sdX * sdY);

		return result;
	}


    
    public byte[] shiftImage( byte[] pixels, int[] position){
    	//position = Array [x,y]
    	byte[] shift_img = new byte[width * height];
    	byte[][] pixels_yx = new byte[height][width];
    	byte[][] pixels_xy = new byte[width][height];
    	byte[][] shiftedImg_yx = new byte[height][width];
    	byte[][] shiftedImg_xy = new byte[width][height];

    	for (int y = 0; y < height ;y++ ){
			for(int x = 0; x < width; x++){
				int idx = (y * width) + x;
				pixels_yx[y][x] = pixels[idx];
			}
		}
    	//shift y
    	if(position[1] < 0){
    		for(int y = -position[1]; y < height; y++){
    			shiftedImg_yx[y] = pixels_yx[y + position[1]];
    		}    			
    	}else if(position[1] >= 0){
    		for(int y = position[1]; y < height; y++){
    			shiftedImg_yx[y - position[1]] = pixels_yx[y];
    		}      			
    	}
    	//転置
    	
		sortXtoY(shiftedImg_yx, pixels_xy);

    	//shift x
    	if(position[0] < 0){
    		for(int x = -position[0]; x < width; x++){
    			shiftedImg_xy[x] = pixels_xy[x + position[0]];
    		}   		
   		}else if(position[0] >= 0){
    		for(int x = position[0]; x < width; x++){
    			shiftedImg_xy[x - position[0]] = pixels_xy[x];
    		}   			
   		}
    	//転置
		sortXtoY(shiftedImg_xy, pixels_yx);
   	
    	for(int y = 0; y < height; y++){
    		for(int x = 0; x < width; x++){
    			int idx = (y * width) + x;
    			shift_img[idx] = pixels_yx[y][x];
    		}
    	}
    	return shift_img;
    }

    public void shiftImage(ImageProcessor imgp, int[] position){
    	imgp.translate((double)position[0], (double)position[1]);
    }
    

    
    public void shiftImageO( byte[] pixels, int[] position){
    	byte[][] pixels_yx = new byte[height][width];
    	byte[][] pixels_xy = new byte[width][height];
    	byte[][] shiftedImg_yx = new byte[height][width];
    	byte[][] shiftedImg_xy = new byte[width][height];

    	for (int y = 0; y < height ;y++ ){
			for(int x = 0; x < width; x++){
				int idx = (y * width) + x;
				pixels_yx[y][x] = pixels[idx];
			}
		}
    	//shift y
    	if(position[1] < 0){
    		for(int y = -position[1]; y < height; y++){
    			shiftedImg_yx[y] = pixels_yx[y + position[1]];
    		}    			
    	}else if(position[1] >= 0){
    		for(int y = position[1]; y < height; y++){
    			shiftedImg_yx[y - position[1]] = pixels_yx[y];
    		}      			
    	}
    	//転置
    	
		sortXtoY(shiftedImg_yx, pixels_xy);

    	//shift x
    	if(position[0] < 0){
    		for(int x = -position[0]; x < width; x++){
    			shiftedImg_xy[x] = pixels_xy[x + position[0]];
    		}   		
   		}else if(position[0] >= 0){
    		for(int x = position[0]; x < width; x++){
    			shiftedImg_xy[x - position[0]] = pixels_xy[x];
    		}   			
   		}
    	//転置
		sortXtoY(shiftedImg_xy, pixels_yx);
   	
    	for(int y = 0; y < height; y++){
    		for(int x = 0; x < width; x++){
    			int idx = (y * width) + x;
    			pixels[idx] = pixels_yx[y][x];
    		}
    	}
    }

    public void shiftImageZ( byte[] pixels, byte[] base_image, int[] position){
    	byte[][] pixels_yx = new byte[height][width];
    	byte[][] pixels_xy = new byte[width][height];
    	byte[][] shiftedImg_yx = new byte[height][width];
    	byte[][] shiftedImg_xy = new byte[width][height];

    	for (int y = 0; y < height ;y++ ){
			for(int x = 0; x < width; x++){
				int idx = (y * width) + x;
				pixels_yx[y][x] = base_image[idx];
			}
		}
    	//shift y
    	if(position[1] < 0){
    		for(int y = -position[1]; y < height; y++){
    			shiftedImg_yx[y] = pixels_yx[y + position[1]];
    		}    			
    	}else if(position[1] >= 0){
    		for(int y = position[1]; y < height; y++){
    			shiftedImg_yx[y - position[1]] = pixels_yx[y];
    		}      			
    	}
    	//転置
    	
		sortXtoY(shiftedImg_yx, pixels_xy);

    	//shift x
    	if(position[0] < 0){
    		for(int x = -position[0]; x < width; x++){
    			shiftedImg_xy[x] = pixels_xy[x + position[0]];
    		}   		
   		}else if(position[0] >= 0){
    		for(int x = position[0]; x < width; x++){
    			shiftedImg_xy[x - position[0]] = pixels_xy[x];
    		}   			
   		}
    	//転置
		sortXtoY(shiftedImg_xy, pixels_yx);
   	
    	for(int y = 0; y < height; y++){
    		for(int x = 0; x < width; x++){
    			int idx = (y * width) + x;
    			pixels[idx] = pixels_yx[y][x];
    		}
    	}
    }

    public void shiftImageC(byte[] base_img, byte[] pixels, int[] position){
    	byte[][] pixels_yx = new byte[height][width];
    	byte[][] pixels_xy = new byte[width][height];
    	byte[][] shiftedImg_yx = new byte[height][width];
    	byte[][] shiftedImg_xy = new byte[width][height];

    	for (int y = 0; y < height ;y++ ){
			for(int x = 0; x < width; x++){
				int idx = (y * width) + x;
				pixels_yx[y][x] = pixels[idx];
			}
		}
    	//shift y
    	if(position[1] < 0){
    		for(int y = -position[1]; y < height; y++){
    			shiftedImg_yx[y] = pixels_yx[y + position[1]];
    		}    			
    	}else if(position[1] >= 0){
    		for(int y = position[1]; y < height; y++){
    			shiftedImg_yx[y - position[1]] = pixels_yx[y];
    		}      			
    	}
    	//転置
    	
		sortXtoY(shiftedImg_yx, pixels_xy);

    	//shift x
    	if(position[0] < 0){
    		for(int x = -position[0]; x < width; x++){
    			shiftedImg_xy[x] = pixels_xy[x + position[0]];
    		}   		
   		}else if(position[0] >= 0){
    		for(int x = position[0]; x < width; x++){
    			shiftedImg_xy[x - position[0]] = pixels_xy[x];
    		}   			
   		}
    	//転置
		sortXtoY(shiftedImg_xy, pixels_yx);
   	
    	for(int y = 0; y < height; y++){
    		for(int x = 0; x < width; x++){
    			int idx = (y * width) + x;
    			base_img[idx] = pixels_yx[y][x];
    		}
    	}
    }
    
    public void changeImp(ImageProcessor current_imp, ImageProcessor reference_imp){
    	int width = current_imp.getWidth();
    	int height = current_imp.getHeight();
    	
    	for(int y = 0; y < height; y++){
    		for(int x = 0; x < width; x++){
    			int value = reference_imp.getPixel(x, y);
    			current_imp.set(x,y,value);
    		}
    	}
    }
    
    public void projectionMax(byte[] base_pixels, byte[] base_image,  byte[] pixels){
    	for(int i = 0; i < pixels.length; i ++){
        	int p_value = base_pixels[i]&0xFF;
        	int b_value = base_image[i]&0xFF;
        	if(p_value < b_value){
        		p_value = b_value;
        	}
    		pixels[i] = (byte)p_value;
    	}
    }
    
    public void projectionMaxS(short[] base_pixels, short[] base_image,  short[] pixels){
    	for(int i = 0; i < pixels.length; i ++){
        	int p_value = base_pixels[i];
        	int b_value = base_image[i];
        	if(p_value < b_value){
        		p_value = b_value;
        	}
    		pixels[i] = (short)p_value;
    	}
    }
    
    public void projectionMax(ImageProcessor nonMerge, ImageProcessor base_image, ImageProcessor output){

 
    	int width = output.getWidth();
    	int height = output.getHeight();
    	
    	for(int y = 0; y < height; y++ ){
    		for(int x = 0; x < width; x++){
    			int p_value = nonMerge.getPixel(x, y);
    			int b_value = base_image.getPixel(x, y);
    			output.set(x,y,b_value);
    			
    			if(p_value > b_value){
    				output.set(x,y,p_value);
    			}
    		}
    	}
    	
	
    }
    
    
    public int ave(int[] a){
    	int sum = 0;
    	int ave = 0;
    	for(int i = 0; i < a.length; i++){
    		sum += a[i];
    	}
    	ave = round(sum / a.length);
    	return ave;
    }

	//xおよびy軸圧縮画像から相関係数を比較
	public double[] getPosition2(ImageProcessor ip1, ImageProcessor ip2){
		//System.out.println("getPosition2");

		double[] position_array = {0,0};
		
		ImageProcessor[] compress_data1 = compress(ip1);
		ImageProcessor[] compress_data2 = compress(ip2);

		
		
		double[] data1_x_array = new double[height];
		double[] data1_y_array = new double[width];
		
		double[] data2_x_array = new double[height];
		double[] data2_y_array = new double[width];		

		
		for(int y = 0; y < height; y++){
			data1_x_array[y] = (double)compress_data1[0].getPixel(0, y);
			data2_x_array[y] = (double)compress_data2[0].getPixel(0, y);
		}
		
		for(int x = 0; x < width; x++){
			data1_y_array[x] = (double)compress_data1[1].getPixel(x, 0);
			data2_y_array[x] = (double)compress_data2[1].getPixel(x, 0);	
		}
		
		double coc = 0;
		int shift_x = 0;
		int shift_y = 0;

		int y_range = height / 4; //とりあえずの値。
		int x_range = width / 4;
		
		
		for(int i = 0; i < y_range; i++){
			double ccoc = 0;
			
			double[] cal_data1_p = Arrays.copyOfRange(data1_x_array, i, height);
			double[] cal_data1_n = Arrays.copyOfRange(data1_x_array, 0, height - i);
			
			double[] cal_data2_p = Arrays.copyOfRange(data2_x_array, i, height);
			double[] cal_data2_n = Arrays.copyOfRange(data2_x_array, 0, height - i);
						
			ccoc = getCOC(cal_data1_p, cal_data2_n);
			if(ccoc > coc){
				coc = ccoc;
				shift_y = -i;
			}
			
			ccoc = getCOC(cal_data1_n, cal_data2_p);
			if(ccoc > coc){
				coc = ccoc;
				shift_y = i;
			}
			
		}
		
		
		
		coc = 0;
		for(int i = 0; i < x_range; i++){
			double ccoc = 0;
			
			double[] cal_data1_p = Arrays.copyOfRange(data1_y_array, i, width);
			double[] cal_data1_n = Arrays.copyOfRange(data1_y_array, 0, width - i);
			
			double[] cal_data2_p = Arrays.copyOfRange(data2_y_array, i, width);
			double[] cal_data2_n = Arrays.copyOfRange(data2_y_array, 0, width - i);
						
			ccoc = getCOC(cal_data1_p, cal_data2_n);
			if(ccoc > coc){
				coc = ccoc;
				shift_x = -i;
			}
			
			ccoc = getCOC(cal_data1_n, cal_data2_p);
			if(ccoc > coc){
				coc = ccoc;
				shift_x = i;
			}
			
		}
		
		
		
		position_array[0] = shift_x;
		position_array[1] = shift_y;

		//System.out.println("shift:" + shift_x + "," + shift_y);
		return position_array;
	}

	public ImageProcessor[] compress(ImageProcessor imgp){
		
		int img_width = imgp.getWidth();
		int img_height = imgp.getHeight();	
		int depth = imgp.getBitDepth();
		

		int[] x_pixel_i = new int[img_width];
		short[] x_pixel_s = new short[img_width];
		short[] x_pixel_b = new short[img_width];

		int[] y_pixel_i = new int[img_height];
		short[] y_pixel_s = new short[img_height];
		short[] y_pixel_b = new short[img_height];
		
		int ini_value = 0;
		Arrays.fill(x_pixel_i, ini_value);
		Arrays.fill(x_pixel_s, (short)ini_value);
		Arrays.fill(x_pixel_b, (byte)ini_value);

		Arrays.fill(y_pixel_i, ini_value);
		Arrays.fill(y_pixel_s, (short)ini_value);
		Arrays.fill(y_pixel_b, (byte)ini_value);
		
		Object x_data = x_pixel_i;
		Object y_data = y_pixel_i;
		
		if(depth == 16){
			x_data = x_pixel_s;
			y_data = y_pixel_s;
		}else if(depth == 8){
			x_data = x_pixel_b;
			y_data = y_pixel_b;
		}

		ImageProcessor x_comp;
		ImageProcessor y_comp;


		ImageStack x_buff = new ImageStack(1,img_height);	
		x_buff.addSlice("", y_data);
		ImagePlus x_imp = new ImagePlus();
		x_imp.setStack(x_buff);
		x_comp = x_imp.getProcessor();


		
		ImageStack y_buff = new ImageStack(img_width,1);	
		y_buff.addSlice("", x_data);
		ImagePlus y_imp = new ImagePlus();
		y_imp.setStack(y_buff);
		y_comp = y_imp.getProcessor();


		
		for(int x = 0; x < img_width ; x++){
			y_comp.set(x,0,0); //初期値
			int buff = 0;
			for(int y = 0; y < img_height; y++){
				int v = imgp.get(x,y);
				buff = buff + v;
				//if(y_comp.get(x,0) < v){ //最大輝度画像
				//	y_comp.set(x,0,v);
				//}
				
			}
			y_comp.set(x, 0, (buff/img_height)); //平均輝度画像
		}

		
		for(int y = 0; y < img_height; y++){
			x_comp.set(0,y,0); //初期値
			int buff = 0;
			for(int x = 0; x < img_width; x++){
				int v = imgp.get(x,y);
				buff = buff + v;
				//if(x_comp.get(0,y) < v){ //最大輝度画像
				//	x_comp.set(0,y,v);
				//}
				
			}

			x_comp.set(0, y, (buff/img_width)); //平均輝度画像
		}	


		
		ImageProcessor[] return_img = {x_comp, y_comp}; // {x,y}
		
		return return_img;
	}
	
}