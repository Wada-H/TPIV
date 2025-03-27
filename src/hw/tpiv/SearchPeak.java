package hw.tpiv;

import ij.process.ImageProcessor;

public class SearchPeak {
	
	ImageProcessor ip;
	int width;
	int height;
	int[] min_peak_point = {0,0};

	public SearchPeak(ImageProcessor img){

		ip = img;
		width = img.getWidth();
		height = img.getHeight();
	}
	
	public int[] getPeak(){
		return searchPeak();
	}

	public double[] getPealDouble(){
		return gaussianPeakSearch(findMax());
	}


	public int[] searchPeak(){
		float buff_value_f = 0.0f;
		boolean check = false;

		int[] peak_point = {0,0};


		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				
				float value_f = ip.getPixelValue(x, y);

				if(value_f == buff_value_f){ //Å‘å’l‚ª‚Q‚ÂˆÈã‚ ‚éê‡‚ÍˆÚ“®‚µ‚Ä‚¢‚È‚¢‚Æ‚Ý‚È‚·‚½‚ß
					check = true;
				}
				
				if(value_f > buff_value_f){
					peak_point[0] = x;
					peak_point[1] = y;
					//buff_value = value;
					buff_value_f = value_f;
					check = false;
 				}
			}
		}

		if(check){
			peak_point[0] = width / 2;
			peak_point[1] = height / 2;
		}else if((Math.abs(peak_point[0] - (width / 2)) > (width / 10)) | (Math.abs(peak_point[1] - (height / 2))> (height / 10))){
			peak_point[0] = width / 2;
			peak_point[1] = height / 2;
		}


		return peak_point;
	}




	public int[] findMax() {
		int[] position = {0, 0};
		float max = ip.getPixelValue(0, 0);

		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				if (ip.getPixelValue(i, j) > max) {
					max = ip.getPixelValue(i, j);
					position[0] = i;
					position[1] = j;
				}
			}
		}
		return position;
	}



	public double[] gaussianPeakSearch(int[] peak_point) {
		double[] position = new double[2];
		int x = peak_point[0];
		int y = peak_point[1];

		if ((x == 0) || (x == (width - 1)) || (y == 0) || (y == (height - 1))) {
			position[0] = x;
			position[1] = y;
		} else {

			double buffV1 = Math.log(ip.getPixel(x - 1, y));
			double buffV2 = Math.log(ip.getPixel(x + 1, y));
			double buffV3 = Math.log(ip.getPixel(x, y - 1));
			double buffV4 = Math.log(ip.getPixel(x, y + 1));
			double buffV5 = Math.log(ip.getPixel(x, y));

			position[0] = x + ((buffV1 - buffV2) / ((2 * buffV1) - (4 * buffV5) + (2 * buffV2)));
			position[1] = y + ((buffV3 - buffV4) / ((2 * buffV3) - (4 * buffV5) + (2 * buffV4)));

		}
		return position;
	}
	
}