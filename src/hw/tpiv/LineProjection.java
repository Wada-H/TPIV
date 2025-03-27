package hw.tpiv;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;


public class LineProjection extends Correlation{
	ImagePlus imp;
	
	int width;
	int height;
	
	double[] before_data_x_axis;
	double[] before_data_y_axis;
	int before_img_id;
	
	public LineProjection(ImagePlus img){
		imp = img;
		width = img.getWidth();
		height = img.getHeight();		
	}
	
	
	//xおよびy軸圧縮画像から相関係数を比較
	public double[] getPosition(int first_stack_id, int second_stack_id, int extension){
		ImageProcessor ip1 = imp.getStack().getProcessor(first_stack_id);
		ImageProcessor ip2 = imp.getStack().getProcessor(second_stack_id);

		
		double[] position_array = {0.0,0.0};

		double[] data1_x_axis = before_data_x_axis;
		double[] data1_y_axis = before_data_y_axis;
		
		if((data1_x_axis == null) ||(before_img_id != first_stack_id)){
			data1_x_axis = makeProjectionXaxis(ip1 ,extension);
			data1_y_axis = makeProjectionYaxis(ip1, extension);
		}
		

		
		double[] data2_x_axis = makeProjectionXaxis(ip2, extension);
		double[] data2_y_axis = makeProjectionYaxis(ip2, extension);

        //double[] data2_x_axis = makeProjectionXaxisF(ip2, extension);
        //double[] data2_y_axis = makeProjectionYaxisF(ip2, extension);

		before_data_x_axis = data2_x_axis;
		before_data_y_axis = data2_y_axis;
		before_img_id = second_stack_id;


		int shift_x = getShiftP(data1_x_axis, data2_x_axis);
		int shift_y = getShiftP(data1_y_axis, data2_y_axis);
					
		position_array[0] = shift_x / (double)extension;
		position_array[1] = shift_y / (double)extension;

		//System.out.println("shift:" + shift_x + "," + shift_y);
        //System.out.println("shift/e:" + position_array[0] + "," + position_array[1]);

        return position_array;
	}
	
		
	private double[] makeProjectionYaxis(ImageProcessor ip, int extension){
		double[] result = new double[height * extension];
		IntStream y_stream = IntStream.range(0, height * extension);
		double dev = (double)extension;


		y_stream.parallel().forEach(y -> {

                double[] buff = new double[width];
			for(int x = 0; x < width; x++){
			    if(extension > 1) {
                    buff[x] = ip.getInterpolatedValue(x, y / dev);
                }else {
                    buff[x] = ip.getPixel(x, y);
                }
			}
			result[y] = getAve(buff);

		});


		return result;
	}


	private double[] makeProjectionXaxis(ImageProcessor ip, int extension){
		double[] result = new double[width * extension];
		IntStream x_stream = IntStream.range(0, width * extension);
		double dev = (double)extension;

		x_stream.parallel().forEach(x -> {

			double[] buff = new double[height];
			for(int y = 0; y < height; y++){
			    if(extension > 1) {
                    buff[y] = ip.getInterpolatedValue(x / dev, y);
                }else {
                    buff[y] = ip.getPixel(x, y);
                }
			}
			result[x] = getAve(buff);
		});


		return result;
	}


	//下記のように省メモリ、速度優先にすると精度が落ちる、、、DoubleProcessorみたいなのがあれば、、、
    private double[] makeProjectionXaxisF(ImageProcessor ip, int extension){
        double[] result = new double[width * extension];
        float[] buffF = new float[width];
        IntStream x_stream = IntStream.range(0, width);

        x_stream.parallel().forEach(x -> {
            double[] buff = new double[height];
            for(int y = 0; y < height; y++){
                buff[y] = ip.getPixel(x, y);
            }
            result[x] = getAve(buff);
            buffF[x] = (float)result[x];
        });

        if(extension > 1) {
            ImageProcessor buffp = new FloatProcessor(width, 1, buffF);
            IntStream xStream = IntStream.range(0, width * extension);
            xStream.parallel().forEach(ex -> {
                result[ex] = buffp.getInterpolatedValue(ex / extension,0);
            });
        }

        return result;
    }

    //下記のように省メモリ、速度優先にすると精度が落ちる、、、
    private double[] makeProjectionYaxisF(ImageProcessor ip, int extension){
        double[] result = new double[height * extension];
        float[] buffF = new float[height];
        IntStream y_stream = IntStream.range(0, height);

        y_stream.parallel().forEach(y -> {
            double[] buff = new double[width];
            for(int x = 0; x < width; x++){
                buff[x] = ip.getPixel(x, y);
            }
            result[y] = getAve(buff);
            buffF[y] = (float)result[y];
        });


        if(extension > 1) {
            ImageProcessor buffp = new FloatProcessor(1, height, buffF);

            IntStream yStream = IntStream.range(0, height * extension);
            yStream.parallel().forEach(ey -> {
                result[ey] = buffp.getInterpolatedValue(0,ey / extension);
            });
        }

        return result;
    }


	private double getAve(double[] value_array){
		DoubleStream d_stream = DoubleStream.of(value_array);
		double result = d_stream.average().getAsDouble();
		return result;
	}
	
	private int getShift(double[] array1, double[] array2){
		int shift_value = 0;
		int length = array1.length;
		int range = (length / 4) ;
		double coc = 0;
		
		for(int i = 0; i < range; i++){ 
			double ccoc = 0;
			
			double[] cal_data1_p = Arrays.copyOfRange(array1, i, length);
			double[] cal_data1_n = Arrays.copyOfRange(array1, 0, length - i);
			
			double[] cal_data2_p = Arrays.copyOfRange(array2, i, length);
			double[] cal_data2_n = Arrays.copyOfRange(array2, 0, length - i);
						
			ccoc = getCOC(cal_data1_p, cal_data2_n);
			if(ccoc > coc){
				coc = ccoc;
				shift_value = -i;
			}
			
			ccoc = getCOC(cal_data1_n, cal_data2_p);
			if(ccoc > coc){
				coc = ccoc;
				shift_value = i;
			}
			
		}

		return shift_value;
	}

	private int getShiftP(double[] array1, double[] array2){
		int shift_value = 0;
		int length = array1.length;
		int range = (length / 4) ;

		ConcurrentHashMap<Double, Integer> hashMap = new ConcurrentHashMap<>();//< correlation, shift position>
		IntStream intStream = IntStream.range(0, range);
		intStream.parallel().forEach(i -> {

			double[] cal_data1_p = Arrays.copyOfRange(array1, i, length);
			double[] cal_data1_n = Arrays.copyOfRange(array1, 0, length - i);

			double[] cal_data2_p = Arrays.copyOfRange(array2, i, length);
			double[] cal_data2_n = Arrays.copyOfRange(array2, 0, length - i);

			double minus = getCOC(cal_data1_p, cal_data2_n);
			double plus = getCOC(cal_data1_n, cal_data2_p);

			hashMap.put(minus, -i);
			hashMap.put(plus, i);

		});

		Double maxKey = hashMap.keySet().parallelStream().max(Comparator.naturalOrder()).get();
        //Double maxKey = hashMap.keySet().stream().max(Comparator.naturalOrder()).get();

        if(!maxKey.isNaN()) {

			//if(maxKey < 0.4){ //足切りの移動距離, xかyをおこなうとどちらもするべきだが？？
			//	shift_value = 0;
			//}else {
				shift_value = hashMap.get(maxKey);
			//}
		}else{
            shift_value = 0;
        }
		return shift_value;


	}


}