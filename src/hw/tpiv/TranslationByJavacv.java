package hw.tpiv;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.bytedeco.javacv.JavaCV;
import org.bytedeco.opencv.opencv_core.IplImage;

import java.nio.FloatBuffer;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.cvMatchTemplate;

//import org.bytedeco.javacpp.opencv_core.*;
//import static org.bytedeco.javacpp.opencv_core.*;
//import static org.bytedeco.javacpp.opencv_imgproc.cvMatchTemplate;
//import org.bytedeco.javacv.Java2DFrameUtils;


/**
 * OpenCv, javacvを使用したTemplate matchingでshift量を算出
 * 参考 : https://sites.google.com/site/qingzongtseng/template-matching-ij-plugin
 *
 * 20200805 Fiji : javacv ver 1.4.2 使用のため 1.5.3で作成しているとimportの場所に違いがでて動かないことが判明。1.4.2を参照してimport先を設定すると動作することを確認した。
 *          Java2DFrameUtilsが同じ場所なので同時記述すると矛盾が生じる。-> 14と15に分けてコンパイルするしかないようだ。
 *
 * 20200807 Java2DFrameUtilsを使わずに変換できると、一つのコードですむかも。
 *
 */


public class TranslationByJavacv {

    JavaCV javaCV = new JavaCV();
    int javaCvVersion;

    ImagePlus tempImg;
    ImagePlus targetImg;

    int width;
    int height;
    Roi tempROI;

    ImageProcessor resultImg;

    //CV_TM_SQDIFF = 0, CV_TM_SQDIFF_NORMED = 1, CV_TM_CCORR = 2, CV_TM_CCORR_NORMED = 3, CV_TM_CCOEFF = 4, CV_TM_CCOEFF_NORMED = 5;


    public TranslationByJavacv(ImagePlus target, Roi template){ //with ROI
        Package objPackage = javaCV.getClass().getPackage();
        String name = objPackage.getSpecificationTitle();
        javaCvVersion = Integer.valueOf(objPackage.getSpecificationVersion().replaceAll("\\.", ""));
        System.out.println("JavaCv version: " + javaCvVersion);

        targetImg = target;
        tempROI = template;

        target.setRoi(template);
        tempImg = new Duplicator().run(target);

    }

    public double[] getPosition(int first_stack_id, int second_stack_id, int extension, int method){
        double[] xyPosition = {0.0, 0.0};


        //このように分岐不可
        //if(javaCvVersion < 150) {
            //resultImg = this.findPosition14(targetImg.getStack().getProcessor(second_stack_id), tempImg.getStack().getProcessor(first_stack_id), method);

        //}else{
            resultImg = this.findPosition15(targetImg.getStack().getProcessor(second_stack_id), tempImg.getStack().getProcessor(first_stack_id), method);
        //}

        SearchPeak searchPeak = new SearchPeak(resultImg);

        double[] centerPosition = searchPeak.getPealDouble();

        xyPosition[0] = -(tempROI.getXBase() - centerPosition[0]);
        xyPosition[1] = -(tempROI.getYBase() - centerPosition[1]);

        //System.out.println("TranslationByJavacv : " + xyPosition[0] + ", " + xyPosition[1]);

        return xyPosition;
    }



    /*14
    public ImageProcessor findPosition14(ImageProcessor target, ImageProcessor template, int method) {

        int targetWidth = target.getWidth();
        int targetHeight = target.getHeight();
        int tempWidth = template.getWidth();
        int tempHeight = template.getHeight();


        IplImage targetIpl = Java2DFrameUtils.toIplImage(target.getBufferedImage());
        IplImage tempIpl = Java2DFrameUtils.toIplImage(template.getBufferedImage());

        IplImage resultIpl = cvCreateImage(cvSize((targetWidth - tempWidth + 1), (targetHeight - tempHeight + 1)), 32,1);

        cvMatchTemplate(targetIpl, tempIpl, resultIpl, method);

        //ImagePlus result = new ImagePlus("", Java2DFrameUtils.toBufferedImage(resultIpl)); //ここがだめ

        FloatBuffer fb = resultIpl.createBuffer();
        float[] f = new float[resultIpl.width() * resultIpl.height()];
        fb.get(f, 0, f.length);

        ImageProcessor resultFp = new FloatProcessor(resultIpl.width(), resultIpl.height(), f, null);

        cvReleaseImage(resultIpl);
        tempIpl.release();
        targetIpl.release();

        return resultFp;
    }
    */

    ///*15
    public ImageProcessor findPosition15(ImageProcessor target, ImageProcessor template, int method) {
        int targetWidth = target.getWidth();
        int targetHeight = target.getHeight();
        int tempWidth = template.getWidth();
        int tempHeight = template.getHeight();

        IplImage targetIpl = org.bytedeco.javacv.Java2DFrameUtils.toIplImage(target.getBufferedImage());
        IplImage tempIpl = org.bytedeco.javacv.Java2DFrameUtils.toIplImage(template.getBufferedImage());


        IplImage resultIpl = cvCreateImage(cvSize((targetWidth - tempWidth + 1), (targetHeight - tempHeight + 1)), IPL_DEPTH_32F,1);

        cvMatchTemplate(targetIpl, tempIpl, resultIpl, method);

        //ImagePlus result = new ImagePlus("", Java2DFrameUtils.toBufferedImage(resultIpl)); //ここがだめ

        FloatBuffer fb = resultIpl.createBuffer();
        float[] f = new float[resultIpl.width() * resultIpl.height()];
        fb.get(f, 0, f.length);

        ImageProcessor resultFp = new FloatProcessor(resultIpl.width(), resultIpl.height(), f, null);

        cvReleaseImage(resultIpl);
        tempIpl.release();
        targetIpl.release();

        return resultFp;
    }
    ///*/


}
