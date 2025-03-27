import hw.tpiv.TPIV;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.io.SaveDialog;
import ij.plugin.frame.PlugInFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


//import com.amd.aparapi.*;

/*
 * 20150221
 * おおかたの処理にめどが付いたが、解決できない問題
 * 	Overlayのclose?問題。
 * 	->最初にTの少ない画像を用いて処理した後、それよりもTの多い画像を用いた場合なぜかOverlayに使用した配列が保持されていて配列エラーがでる。
 * 	->これはImageJのバグっぽいが、、、、ImagePlusをcloseした際にOverlayもcloseしてほしいのだが、、、
 * 	->そもそもOverlayにclose機能がない。ImagePlusにsetメソッドはあるが、removeメソッドがない
 * 	->確認、ZahyoHyperでも同様に起こる。(TPIVのデータが表示されてしまったりもする)
 *  この問題はかなり厳しい。ImageJに報告するべきか。 ->英語だ、、、
 *  !!! なんと、setOverlay(null)をすることで解除できる！！！　なんだそりゃぁ
 */

/* version
 構想 : 20150519
 1.00 : 20150528
 2.0 : 20151217
 3.0 : 20180613
 */

/* 20150603
 * GUP使用するAMD aparapi を使ってみる。
 * うーん、使用したほうが単純計算において5-30倍ほど遅い。
 * Intel Core i7 2.8GHz
 * ATI Radeon HD 4850
 * 
 * 20151007
 * 2-10倍CPUのほうが速い。
 * Intel Core i7 4 GHz
 * AMD Radeon R9 M295
 * 
 * 20151214
 * viewとmethodの分離を考える。また、SwingWorker内のmulti thread化を考える。
 * 20151215
 * PIV処理の分離がある程度出来た。このファイル上の重複methodを廃止の方向へ
 * 20151217
 * steamを使った並列化完了。stack画像のみを使用することに限定する。(c,zを考慮すると、RGB表示できなくなる。また、HyperStackでないとエラーが出る)
 * 20151218
 *
 * 20180613
 * LineProjection methodのsubpixel化に伴いsubpixel化。
 *
 * 20180620
 * LineProjectionの不具合(最大相関値がNaNを取った場合の処理)の解消
 *
 * 20180718
 * 角度を色分けする機能を追加。HSBに沿った色相。0 = red, 60 = yellow, 180 = cyan, ...
 * H = 角度, S = max value, B = lengthを10で割った値で、1を超える分は1とする。
 *
 * 20189719
 * 処理をするごとにごく一部のshif値が少しだけ違う場合があることに気がつく。
 *  ->原因としてはsep_imgを作る際の並列処理にあるよう。とりえあえずparallelを削除で。
 *
 * 20190403
 * 1.52nのduplicate()仕様変更による修正
 *
 * 20200914
 * saveTSVにlength, radianを記録するmethodを追加 *ragianの符号チェエクができていない
 * 表示される矢印の色を振り分ける際に以上、未満の不備により一部正しい色が表現されていない不具合の修正
 * 表示されるカラーボックスを表示する機能を追加(length用のみ)
 *
 * 20200915
 * saveTSVのLengthではextend valueの値が反映された長さになっているためこれを修正
 * saveTSVのAngleではRadianの値が直感と180度対象にずれているため修正
 *
 * 20200917
 * 豊岡さんより相対表示ではなく絶対表示での色つけを残しておくことを希望。
 * おそらくは短いものが目立つことがいやなのかなと。
 *
 * 20201022
 * 一部コード(compartRoiColor2)の見直し。*結果は同じ
 *
 * 20201102
 * 一部コード見直し(LineProjection.getShiftP : 0.4の足切りを廃止)、この足切りより、大きな外れ値の判定をどうするかのほうが重要
 * 座標保存時もlength, angle同様の並び順に変更
 *
 * 20210831
 * 坪井さん(近藤武史研)より以前のような矢印の色を絶対表示の機能も欲しいとの要望あり。
 * また、ColorBarに数値が入っていると嬉しいとこのこと。
 * 以前豊岡さんにも同じことを言われているので時間のあるときに対処する。
 *  -> UIにColor Method のComboBoxを追加(Absolute, Relative, Hue)。
 * 	-> 以前のコードを一部変更して絶対値(任意の最大値)を8分割して色分けするようにようにした。
 * 	-> Methodを変更すると、Length limit <-> Length ratio limitが自動で切り替わるようにしたい。 ->  20210902 fieldのenableの切り替えで対応
 *	-> Hueタイプの表示の修正を行い(ratio数の逆転)見栄えが向上。これを標準とする。
 *
 * 20210929
 * 坪井さんの依頼で座標保存時のフォーマットが前のほうがいいとの連絡あり（解析に作成したプログラムの関係で）
 * とりあえず、坪井バージョンとして座標に関しての保存を以前のタイプのものを作ることで対応する。
 */




public class TPIV_ extends PlugInFrame implements ImageListener, WindowListener, MouseListener {

	TPIV tpiv;
	
	long start;
	long end;

	ImagePlus imp = null;

	ImagePlus new_imp = null;

	int slice = 0;

	String title = null;
	String dir = null;


	// / Dialog ////
	JTextField window_width_field;
	JTextField window_height_field;
	JTextField overlap_field;
	JTextField extendV_field;
	JTextField subpixelField;
	JTextField limitValueField;
	JTextField maxLengthLimitValueField;

	JCheckBox overwrite_checkbox;
	JCheckBox withOimage_checkbox;
	JCheckBox correctByM_checkbox;
	JCheckBox angleColor_checkbox;

	JComboBox colorMethodBox;
	String[] colorMethods = {"Hue", "Absolute", "Relative"};

	JComboBox saveMethodsBox;
	String[] saveMethods = {"Coordinate", "Length", "Angle"};

	JButton showColorBoxButton;
	JButton calc_button;
	JButton save_button;
	JProgressBar p_bar;

	// // PIV 基本設定 ////
	int windowWidth = 16;
	int windowHeight = 16;
	int overlap = 50; // 50%
	int extendValue = 5;
	double limitValue = 1.0;
	double maxLengthLimitValue = 8.0;
	int subpixelValue = 10;
	int colorMethod = 0;

	Overlay[] overlay; //slice


	// / save tsv ////
	String save_file_name;

	public TPIV_() {
		super("TPIV_ver3.3_20210903");
		//super("TPIV_ver3.3_Type:Tsuboi");

		// 現在のイメージの取得
		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.noImage();
			return;
		}

		 if(imp.isHyperStack()){
		 IJ.showMessage("Please convert to Stack");
		 	return;
		 }

		slice = imp.getStackSize();
		
		title = imp.getTitle();
		new_imp = imp.duplicate();

		if (imp.getOriginalFileInfo() != null) { // 新しく作った画像にはFileInfoが設定されていない
			dir = imp.getOriginalFileInfo().directory; // 初期値をオリジナルファイルと同じ場所に。
		} else {
			dir = "home";
		}

		
		if (slice < 2) {
			IJ.showMessage("Time series stack image is necessary.");
			return;
		}

		setOverlay();
		setListener();
		showPanel(); // 基本設定パネル表示

	}

	public void setOverlay() {
		overlay = new Overlay[slice];

		for (int ct = 0; ct < slice; ct++) {
			overlay[ct] = new Overlay();
		}				

	}

	public void clearOverlay() {
		
		for (int ct = 0; ct < slice; ct++) {
			overlay[ct].clear();
		}	
	}

	public void setListener() {
		this.addWindowListener(this);
		imp.getWindow().addWindowListener(this);
		ImagePlus.addImageListener(this);

	}

	public void makePIVimage() {
		
		SwingWorker<String, Double> worker = new SwingWorker<String, Double>() {

			@Override
			public String doInBackground() {

				new_imp = tpiv.makePIVimage(withOimage_checkbox.isSelected(), overwrite_checkbox.isSelected(), correctByM_checkbox.isSelected(), angleColor_checkbox.isSelected());
				overlay = tpiv.getOverlay();
				return "done";
			}



			@Override
			public void done() {

				IJ.showProgress(1);
				new_imp.setT(1);
				new_imp.setOverlay(overlay[0]);
				new_imp.setTitle(title.replaceAll(".tif", "") + "_TPIV");
				new_imp.show();

				end = System.currentTimeMillis();
				System.out.println((end - start) + "msec");
				IJ.showStatus("elapsed time is " + (end - start) + "msec");
				
			}

		};

		worker.execute();

		boolean check = worker.isDone();
		while (check == true) {
			check = worker.isDone();
		}


	}


	public void showPanel() {

		FlowLayout gd_layout = new FlowLayout();
		gd_layout.setAlignment(FlowLayout.RIGHT);

		JPanel gd_panel = new JPanel(gd_layout);
		gd_panel.setPreferredSize(new Dimension(300, 360));

		// /////// setting ////
		JPanel setting_panel = new JPanel((new GridLayout(12, 2)));
		setting_panel.setAlignmentX(JPanel.BOTTOM_ALIGNMENT);

		JLabel window_width_label = new JLabel("Window width (pixel) :");
		window_width_label.setVerticalAlignment(JLabel.CENTER);
		window_width_label.setHorizontalAlignment(JLabel.CENTER);

		window_width_field = new JTextField(String.valueOf(windowWidth), 1);

		JLabel window_height_label = new JLabel("Window height (pixel) :");
		window_height_label.setVerticalAlignment(JLabel.CENTER);
		window_height_label.setHorizontalAlignment(JLabel.CENTER);

		window_height_field = new JTextField(String.valueOf(windowHeight), 1);

		JLabel overlap_label = new JLabel("Orverlap (%) 0-99 :"); // overlap100%は完全一致でまずいはず。
		overlap_label.setVerticalAlignment(JLabel.CENTER);
		overlap_label.setHorizontalAlignment(JLabel.CENTER);

		overlap_field = new JTextField(String.valueOf(overlap), 1);

		JLabel extend_label = new JLabel("Extend Value (3-) :"); // 拡張伸長倍率
		extend_label.setVerticalAlignment(JLabel.CENTER);
		extend_label.setHorizontalAlignment(JLabel.CENTER);
        extendV_field = new JTextField(String.valueOf(extendValue), 1);

        JLabel colorMethodLabel = new JLabel("Color Method : ");
        colorMethodLabel.setVerticalAlignment(JLabel.CENTER);
        colorMethodLabel.setHorizontalAlignment(JLabel.CENTER);
		colorMethodBox = new JComboBox();
		for(String s : colorMethods) {
			colorMethodBox.addItem(s);
		}
		colorMethodBox.setSelectedIndex(0);//Hueがデフォルト
		colorMethodBox.addItemListener(new ItemListener() { //切り分けたい場合はItemListnerをimplimentしたclassを別に作る必要がある
			@Override
			public void itemStateChanged(ItemEvent e) {

				if(colorMethodBox.getSelectedIndex() == 0){
					maxLengthLimitValueField.setEnabled(false);
					limitValueField.setEnabled(true);
				}else if(colorMethodBox.getSelectedIndex() == 1){
					maxLengthLimitValueField.setEnabled(true);
					limitValueField.setEnabled(false);
				}else if(colorMethodBox.getSelectedIndex() == 2){
					maxLengthLimitValueField.setEnabled(false);
					limitValueField.setEnabled(true);
				}

			}
		});

		JLabel maxLengthlimitLabel = new JLabel("Max length limit : "); //任意の最大値
		maxLengthlimitLabel.setVerticalAlignment(JLabel.CENTER);
		maxLengthlimitLabel.setHorizontalAlignment(JLabel.CENTER);
		maxLengthLimitValueField = new JTextField(String.valueOf(maxLengthLimitValue), 1);
		maxLengthLimitValueField.setEnabled(false); //初期値はRerativeのため

        JLabel limitLabel = new JLabel("Length ratio limit : "); //矢印の色表示の上限設定
        limitLabel.setVerticalAlignment(JLabel.CENTER);
        limitLabel.setHorizontalAlignment(JLabel.CENTER);
		limitValueField = new JTextField(String.valueOf(limitValue), 1);

		JLabel subPixelLabel = new JLabel("SubPixel value :");
		subPixelLabel.setVerticalAlignment(JLabel.CENTER);
		subPixelLabel.setHorizontalAlignment(JLabel.CENTER);
        subpixelField = new JTextField(String.valueOf(subpixelValue), 1);


		overwrite_checkbox = new JCheckBox("OverWrite", true);
		overwrite_checkbox.setVerticalAlignment(JCheckBox.CENTER);
		overwrite_checkbox.setHorizontalAlignment(JCheckBox.LEFT);

		withOimage_checkbox = new JCheckBox("WithOriginalImage", true);
		withOimage_checkbox.setVerticalAlignment(JCheckBox.CENTER);
		withOimage_checkbox.setHorizontalAlignment(JCheckBox.LEFT);

		correctByM_checkbox = new JCheckBox("CorrectByMedian", true);
		correctByM_checkbox.setVerticalAlignment(JCheckBox.CENTER);
		correctByM_checkbox.setHorizontalAlignment(JCheckBox.LEFT);

		angleColor_checkbox = new JCheckBox("AngleColor", false);
        angleColor_checkbox.setVerticalAlignment(JCheckBox.CENTER);
        angleColor_checkbox.setHorizontalAlignment(JCheckBox.LEFT);

		JLabel blank_label = new JLabel("");

		showColorBoxButton = new JButton("ShowColorBox");
		showColorBoxButton.addMouseListener(this);

		calc_button = new JButton("Calculate");
		calc_button.addMouseListener(this);

		save_button = new JButton("Save TSV");
		save_button.addMouseListener(this);

		saveMethodsBox = new JComboBox();
		for(String s : saveMethods) {
			saveMethodsBox.addItem(s);
		}


		setting_panel.add(window_width_label);
		setting_panel.add(window_width_field);

		setting_panel.add(window_height_label);
		setting_panel.add(window_height_field);

		setting_panel.add(overlap_label);
		setting_panel.add(overlap_field);

		setting_panel.add(extend_label);
		setting_panel.add(extendV_field);

		setting_panel.add(subPixelLabel);
		setting_panel.add(subpixelField);

		setting_panel.add(colorMethodLabel);
		setting_panel.add(colorMethodBox);

		setting_panel.add(maxLengthlimitLabel);
		setting_panel.add(maxLengthLimitValueField);

		setting_panel.add(limitLabel);
		setting_panel.add(limitValueField);

		setting_panel.add(overwrite_checkbox);
		setting_panel.add(withOimage_checkbox);

		setting_panel.add(correctByM_checkbox);
		setting_panel.add(angleColor_checkbox);

		setting_panel.add(saveMethodsBox);
		setting_panel.add(save_button);

		setting_panel.add(showColorBoxButton);
		setting_panel.add(calc_button);

		// ///////////////////////////////////

		gd_panel.add(setting_panel);

		this.add(gd_panel);
		this.pack(); // 推奨サイズのｗindow

		Point imp_point = imp.getWindow().getLocation();
		int imp_window_width = imp.getWindow().getWidth();

		double set_x_point = imp_point.getX() + imp_window_width;
		double set_y_point = imp_point.getY();

		this.setLocation((int) set_x_point, (int) set_y_point);

		this.setVisible(true);// thisの表示
	}

	public boolean showSaveDialog() {

		SaveDialog sd = new SaveDialog("Save tsv file", dir, (title.replaceAll(".tif", "") + "_TPIV"), ".tsv");

		if (sd.getDirectory() == null || sd.getFileName() == null) {
			return false;
		}
		dir = sd.getDirectory(); // save dialog　中に選択したものに変更。
		save_file_name = sd.getFileName(); // save dialog　中に選択したものに変更。
		return true;
	}

	public boolean setValue() {
		// ImagePlus.removeImageListener(this); //前回のlistenerクリア
		
		windowWidth = Integer.valueOf(window_width_field.getText());
		windowHeight = Integer.valueOf(window_height_field.getText());
		extendValue = Integer.valueOf(extendV_field.getText());
		limitValue = Double.valueOf(limitValueField.getText());
		maxLengthLimitValue = Double.valueOf(maxLengthLimitValueField.getText());
		overlap = Integer.valueOf(overlap_field.getText());
		subpixelValue = Integer.valueOf(subpixelField.getText());
		colorMethod = colorMethodBox.getSelectedIndex();

		tpiv.setWindowWidth(windowWidth);
		tpiv.setWindowHeight(windowHeight);
		tpiv.setExtendValue(extendValue);
		tpiv.setColorMethod(colorMethod);
		tpiv.setMaxLengthLimitValue(maxLengthLimitValue);
		tpiv.setLimitValue(limitValue);
		tpiv.setSubPixelValue(subpixelValue);
		tpiv.setOverlap(overlap);
		
		if ((overlap >= 100) || (overlap < 0)) {
			IJ.showMessage("Please Check the Overlap Value (0 <= Value < 100 )");

			return false;
		}


		return true;
	}

	public void saveTSV() {
		//tpiv.saveTSV(dir, save_file_name);//Type:Tsuboiで使用
		tpiv.saveValuesTSV(dir, save_file_name, 2); //2 == coordinate
	}

	public void saveLengthTSV(){
		tpiv.saveValuesTSV(dir, save_file_name, 0); //0 == length
	}

	public void saveRadianTSV(){
		tpiv.saveValuesTSV(dir, save_file_name, 1); //0 == radian
	}


	@Override
	public void imageOpened(ImagePlus imp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void imageClosed(ImagePlus imp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void imageUpdated(ImagePlus ip) {

		int cs = ip.getStackIndex(ip.getC(), ip.getZ(), ip.getT()); //getSlice() = getZ()のため。
		
		if (new_imp != null) {
			if (ip.getID() == new_imp.getID()) {

				if (overlay[cs - 1] != null) {
					new_imp.setOverlay(overlay[cs - 1]);
				}
			}

		}
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println("closed");
	}

	@Override
	public void windowClosing(WindowEvent arg0) {

		if (arg0.getSource() == new_imp.getWindow()) {
			new_imp.setOverlay(null); // overlayの削除

		} else if (arg0.getSource() == imp.getWindow()) {
			new_imp.setOverlay(null); // overlayの削除
			ImagePlus.removeImageListener(this);
			System.out.println("closing");
			this.close();
		} else if (arg0.getSource() == this) {
			new_imp.setOverlay(null); // overlayの削除
			ImagePlus.removeImageListener(this);
			this.close();
			new_imp.close();
		}
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

		if (arg0.getSource() == calc_button) {
			start = System.currentTimeMillis(); // timer start

			clearOverlay();
			imp.setT(1); // 念のため 1枚目にセット

			// /// 以下　main　の処理 /////
			tpiv = new TPIV(imp);

			if (setValue()) {
				makePIVimage();

			}

			
		} else if (arg0.getSource() == save_button) {
			if (new_imp.isVisible()) {
				if (showSaveDialog()) {
					int selectedID = saveMethodsBox.getSelectedIndex();
					if(selectedID == 0) {
						saveTSV();
					}else if(selectedID == 1){
						saveLengthTSV();
					}else if(selectedID == 2){
						saveRadianTSV();
					}

				} else {
					return;
				}
			} else {

				IJ.showMessage("Please Calculate at First");
			}
		} else if(arg0.getSource() == showColorBoxButton){
			tpiv.showColorBox(240, 50);
		}

	}

}