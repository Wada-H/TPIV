package hw.tpiv;

public class Correlation {
	
	public Correlation(){
		
	}
	
    public static double getCOC( double[] x,  double[] y ) { //COC(coefficient of correlation) ->�܂Ƃ߂ď����ق����f�R�����I
    	//r = �ϐ����ƕϐ�y�̋����U / �ϐ�x�̕W���΍� x �ϐ�y�̕W���΍� ->���L�����̂悤�Ȍv�Z�ɂȂ��Ă��邩�s���B

    	double r;
    	int n; //x��y�̑g��
        double  xt,yt,x2t,y2t,xyt,xh,yh,xs,ys;
        n = x.length; xt = 0; yt = 0; xyt = 0; x2t = 0; y2t = 0;
        double xsd,ysd;
        
        for(int i = 0 ; i < n; i++){
             xt += x[i];   yt += y[i]; //���v�l
             x2t += x[i]*x[i];    y2t += y[i]*y[i]; //�e���U�p����
             xyt += x[i]*y[i]; //�����U�p����
        }
        xh = xt/n; //���ϒl
        yh = yt/n;
        xsd=x2t/n-xh*xh; //���U
        ysd=y2t/n-yh*yh; 
        xs = Math.sqrt(xsd); //�W���΍�
        ys = Math.sqrt(ysd);
        r = (xyt/n-xh*yh)/(xs*ys); //���֌W��
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