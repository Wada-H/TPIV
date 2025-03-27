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
 * ���������̏����ɂ߂ǂ��t�������A�����ł��Ȃ����
 * 	Overlay��close?���B
 * 	->�ŏ���T�̏��Ȃ��摜��p���ď���������A�������T�̑����摜��p�����ꍇ�Ȃ���Overlay�Ɏg�p�����z�񂪕ێ�����Ă��Ĕz��G���[���ł�B
 * 	->�����ImageJ�̃o�O���ۂ����A�A�A�AImagePlus��close�����ۂ�Overlay��close���Ăق����̂����A�A�A
 * 	->��������Overlay��close�@�\���Ȃ��BImagePlus��set���\�b�h�͂��邪�Aremove���\�b�h���Ȃ�
 * 	->�m�F�AZahyoHyper�ł����l�ɋN����B(TPIV�̃f�[�^���\������Ă��܂����������)
 *  ���̖��͂��Ȃ茵�����BImageJ�ɕ񍐂���ׂ����B ->�p�ꂾ�A�A�A
 *  !!! �Ȃ�ƁAsetOverlay(null)�����邱�Ƃŉ����ł���I�I�I�@�Ȃ񂾂���႟
 */

/* version
 �\�z : 20150519
 1.00 : 20150528
 2.0 : 20151217
 3.0 : 20180613
 */

/* 20150603
 * GUP�g�p����AMD aparapi ���g���Ă݂�B
 * ���[��A�g�p�����ق����P���v�Z�ɂ�����5-30�{�قǒx���B
 * Intel Core i7 2.8GHz
 * ATI Radeon HD 4850
 * 
 * 20151007
 * 2-10�{CPU�̂ق��������B
 * Intel Core i7 4 GHz
 * AMD Radeon R9 M295
 * 
 * 20151214
 * view��method�̕������l����B�܂��ASwingWorker����multi thread�����l����B
 * 20151215
 * PIV�����̕�����������x�o�����B���̃t�@�C����̏d��method��p�~�̕�����
 * 20151217
 * steam���g�������񉻊����Bstack�摜�݂̂��g�p���邱�ƂɌ��肷��B(c,z���l������ƁARGB�\���ł��Ȃ��Ȃ�B�܂��AHyperStack�łȂ��ƃG���[���o��)
 * 20151218
 *
 * 20180613
 * LineProjection method��subpixel���ɔ���subpixel���B
 *
 * 20180620
 * LineProjection�̕s�(�ő告�֒l��NaN��������ꍇ�̏���)�̉���
 *
 * 20180718
 * �p�x��F��������@�\��ǉ��BHSB�ɉ������F���B0 = red, 60 = yellow, 180 = cyan, ...
 * H = �p�x, S = max value, B = length��10�Ŋ������l�ŁA1�𒴂��镪��1�Ƃ���B
 *
 * 20189719
 * ���������邲�Ƃɂ����ꕔ��shif�l�����������Ⴄ�ꍇ�����邱�ƂɋC�����B
 *  ->�����Ƃ��Ă�sep_img�����ۂ̕��񏈗��ɂ���悤�B�Ƃ肦������parallel���폜�ŁB
 *
 * 20190403
 * 1.52n��duplicate()�d�l�ύX�ɂ��C��
 *
 * 20200914
 * saveTSV��length, radian���L�^����method��ǉ� *ragian�̕����`�F�G�N���ł��Ă��Ȃ�
 * �\���������̐F��U�蕪����ۂɈȏ�A�����̕s���ɂ��ꕔ�������F���\������Ă��Ȃ��s��̏C��
 * �\�������J���[�{�b�N�X��\������@�\��ǉ�(length�p�̂�)
 *
 * 20200915
 * saveTSV��Length�ł�extend value�̒l�����f���ꂽ�����ɂȂ��Ă��邽�߂�����C��
 * saveTSV��Angle�ł�Radian�̒l��������180�x�Ώۂɂ���Ă��邽�ߏC��
 *
 * 20200917
 * �L�������葊�Ε\���ł͂Ȃ���Ε\���ł̐F�����c���Ă������Ƃ���]�B
 * �����炭�͒Z�����̂��ڗ����Ƃ�����Ȃ̂��ȂƁB
 *
 * 20201022
 * �ꕔ�R�[�h(compartRoiColor2)�̌������B*���ʂ͓���
 *
 * 20201102
 * �ꕔ�R�[�h������(LineProjection.getShiftP : 0.4�̑��؂��p�~)�A���̑��؂���A�傫�ȊO��l�̔�����ǂ����邩�̂ق����d�v
 * ���W�ۑ�����length, angle���l�̕��я��ɕύX
 *
 * 20210831
 * �؈䂳��(�ߓ����j��)���ȑO�̂悤�Ȗ��̐F���Ε\���̋@�\���~�����Ƃ̗v�]����B
 * �܂��AColorBar�ɐ��l�������Ă���Ɗ������Ƃ��̂��ƁB
 * �ȑO�L������ɂ��������Ƃ������Ă���̂Ŏ��Ԃ̂���Ƃ��ɑΏ�����B
 *  -> UI��Color Method ��ComboBox��ǉ�(Absolute, Relative, Hue)�B
 * 	-> �ȑO�̃R�[�h���ꕔ�ύX���Đ�Βl(�C�ӂ̍ő�l)��8�������ĐF��������悤�ɂ悤�ɂ����B
 * 	-> Method��ύX����ƁALength limit <-> Length ratio limit�������Ő؂�ւ��悤�ɂ������B ->  20210902 field��enable�̐؂�ւ��őΉ�
 *	-> Hue�^�C�v�̕\���̏C�����s��(ratio���̋t�])���h��������B�����W���Ƃ���B
 *
 * 20210929
 * �؈䂳��̈˗��ō��W�ۑ����̃t�H�[�}�b�g���O�̂ق��������Ƃ̘A������i��͂ɍ쐬�����v���O�����̊֌W�Łj
 * �Ƃ肠�����A�؈�o�[�W�����Ƃ��č��W�Ɋւ��Ă̕ۑ����ȑO�̃^�C�v�̂��̂���邱�ƂőΉ�����B
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

	// // PIV ��{�ݒ� ////
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

		// ���݂̃C���[�W�̎擾
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

		if (imp.getOriginalFileInfo() != null) { // �V����������摜�ɂ�FileInfo���ݒ肳��Ă��Ȃ�
			dir = imp.getOriginalFileInfo().directory; // �����l���I���W�i���t�@�C���Ɠ����ꏊ�ɁB
		} else {
			dir = "home";
		}

		
		if (slice < 2) {
			IJ.showMessage("Time series stack image is necessary.");
			return;
		}

		setOverlay();
		setListener();
		showPanel(); // ��{�ݒ�p�l���\��

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

		JLabel overlap_label = new JLabel("Orverlap (%) 0-99 :"); // overlap100%�͊��S��v�ł܂����͂��B
		overlap_label.setVerticalAlignment(JLabel.CENTER);
		overlap_label.setHorizontalAlignment(JLabel.CENTER);

		overlap_field = new JTextField(String.valueOf(overlap), 1);

		JLabel extend_label = new JLabel("Extend Value (3-) :"); // �g���L���{��
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
		colorMethodBox.setSelectedIndex(0);//Hue���f�t�H���g
		colorMethodBox.addItemListener(new ItemListener() { //�؂蕪�������ꍇ��ItemListner��impliment����class��ʂɍ��K�v������
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

		JLabel maxLengthlimitLabel = new JLabel("Max length limit : "); //�C�ӂ̍ő�l
		maxLengthlimitLabel.setVerticalAlignment(JLabel.CENTER);
		maxLengthlimitLabel.setHorizontalAlignment(JLabel.CENTER);
		maxLengthLimitValueField = new JTextField(String.valueOf(maxLengthLimitValue), 1);
		maxLengthLimitValueField.setEnabled(false); //�����l��Rerative�̂���

        JLabel limitLabel = new JLabel("Length ratio limit : "); //���̐F�\���̏���ݒ�
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
		this.pack(); // �����T�C�Y�̂�indow

		Point imp_point = imp.getWindow().getLocation();
		int imp_window_width = imp.getWindow().getWidth();

		double set_x_point = imp_point.getX() + imp_window_width;
		double set_y_point = imp_point.getY();

		this.setLocation((int) set_x_point, (int) set_y_point);

		this.setVisible(true);// this�̕\��
	}

	public boolean showSaveDialog() {

		SaveDialog sd = new SaveDialog("Save tsv file", dir, (title.replaceAll(".tif", "") + "_TPIV"), ".tsv");

		if (sd.getDirectory() == null || sd.getFileName() == null) {
			return false;
		}
		dir = sd.getDirectory(); // save dialog�@���ɑI���������̂ɕύX�B
		save_file_name = sd.getFileName(); // save dialog�@���ɑI���������̂ɕύX�B
		return true;
	}

	public boolean setValue() {
		// ImagePlus.removeImageListener(this); //�O���listener�N���A
		
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
		//tpiv.saveTSV(dir, save_file_name);//Type:Tsuboi�Ŏg�p
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

		int cs = ip.getStackIndex(ip.getC(), ip.getZ(), ip.getT()); //getSlice() = getZ()�̂��߁B
		
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
			new_imp.setOverlay(null); // overlay�̍폜

		} else if (arg0.getSource() == imp.getWindow()) {
			new_imp.setOverlay(null); // overlay�̍폜
			ImagePlus.removeImageListener(this);
			System.out.println("closing");
			this.close();
		} else if (arg0.getSource() == this) {
			new_imp.setOverlay(null); // overlay�̍폜
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
			imp.setT(1); // �O�̂��� 1���ڂɃZ�b�g

			// /// �ȉ��@main�@�̏��� /////
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