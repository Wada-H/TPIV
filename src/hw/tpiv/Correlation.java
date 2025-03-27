package hw.tpiv;

public class Correlation {
	
	public Correlation(){
		
	}
	
    public static double getCOC( double[] x,  double[] y ) { //COC(coefficient of correlation) ->まとめて書くほうが断然速い！
    	//r = 変数ｘと変数yの共分散 / 変数xの標準偏差 x 変数yの標準偏差 ->下記がそのような計算になっているか不明。

    	double r;
    	int n; //xとyの組数
        double  xt,yt,x2t,y2t,xyt,xh,yh,xs,ys;
        n = x.length; xt = 0; yt = 0; xyt = 0; x2t = 0; y2t = 0;
        double xsd,ysd;
        
        for(int i = 0 ; i < n; i++){
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
	
    
    public double getCOC( Object[] x,  Object[] y ) { //COC(coefficient of correlation)
    	double[] dx = new double[x.length];
    	double[] dy = new double[y.length];
    	for(int i = 0; i < x.length; i++){
    		dx[i] = (double)x[i];
    	}
    	for(int i = 0; i < y.length; i++){
    		dy[i] = (double)y[i];
    	}
    	
    	return getCOC(dx, dy);
    }
	
}