package hw.tpiv;

/**
 * 計算したシフト情報からフーリエ変換より、周波数を得る
 *
 */

import ij.process.FHT;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

public class CalcFFT {

    double[][][][] shiftedArray; //[slice][xPoint][yPoint][xCoordinate, yCoordinate]
    int tLength;
    int xPositionLength;
    int yPositionLength;


    ConcurrentHashMap<Point, Double[]> xCoordinateArray = new ConcurrentHashMap<>();
    ConcurrentHashMap<Point, Double[]> yCoordinateArray = new ConcurrentHashMap<>();

    ConcurrentHashMap<Point, float[]> xPowerArray = new ConcurrentHashMap<>();
    ConcurrentHashMap<Point, float[]> yPowerArray = new ConcurrentHashMap<>();

    public CalcFFT(double inputArray[][][][]){
        shiftedArray = inputArray;
        tLength = inputArray.length;
        xPositionLength = inputArray[0].length;
        yPositionLength = inputArray[0][0].length;

        convertPositionArray();
    }

    public void convertPositionArray() {

        for(int x = 0; x < xPositionLength; x++){
            for(int y = 0; y < yPositionLength; y++){
                Double[] xValueArray = new Double[tLength];
                Double[] yValueArray = new Double[tLength];
                float[] xValues = new float[tLength];
                float[] yValues = new float[tLength];

                for(int t = 0; t < tLength; t++){
                    xValueArray[t] = shiftedArray[t][x][y][0];
                    yValueArray[t] = shiftedArray[t][x][y][1];
                    if(t == 0){
                        xValues[t] = 0.0f;
                        yValues[t] = 0.0f;
                    }else {
                        xValues[t] = xValueArray[t].floatValue();
                        yValues[t] = yValueArray[t].floatValue();
                    }
                }

                FHT fht = new FHT();
                Point p = new Point(x, y);
                xCoordinateArray.put(p, xValueArray);
                yCoordinateArray.put(p, yValueArray);

                //float[] test = fht.fourier1D(xValues, FHT.HAMMING);
                //for(int a = 0; a < test.length; a++){
                //    System.out.println(test[a]);
                //}

                xPowerArray.put(p, fht.fourier1D(xValues, FHT.HAMMING)); //Rのfftでは自前で窓関数をかける必要がありそう。
                yPowerArray.put(p, fht.fourier1D(yValues, FHT.HAMMING)); //参考サイト : https://cattech-lab.com/science-tools/fft/
                //参考サイトと数値の値や波形に若干の差があるが、ほぼ同様の波形になった。使用する配列データの値がそれぞれ違ったため違ったものだった。
                //特に最初の値が座標値でそれ以降がシフト値であること、座標値をプラスした値であったことなどが影響している。

                //xPowerArray.put(p, xValues); //Rのfftでは自前で窓関数をかける必要がありそう。
                //yPowerArray.put(p, yValues); //参考サイト : https://cattech-lab.com/science-tools/fft/

            }
        }

    }


    public float[] getPowerSpectrumX(Point p){
        return  xPowerArray.get(p);
    }

    public float[] getPowerSpectrumY(Point p){
        return yPowerArray.get(p);
    }
}
