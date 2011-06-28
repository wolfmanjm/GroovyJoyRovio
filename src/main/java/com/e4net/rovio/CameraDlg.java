package com.e4net.rovio;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CameraDlg extends JDialog {
    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	CamEventListener listener= null;
    JPanel panel1 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JLabel vidwin = new JLabel();
    JPanel jPanel1 = new JPanel();
    JButton jButton1 = new JButton();
    JButton jButton2 = new JButton();
    JButton jButton3 = new JButton();
    JButton jButton4 = new JButton();
    JButton jButton5 = new JButton();
    JButton jButton6 = new JButton();
    JButton jButton7 = new JButton();
    JButton jButton8 = new JButton();

    public CameraDlg(Frame frame, String title, boolean modal) {
        super(frame, title, modal);
        try {
            jbInit();
            pack();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public CameraDlg(String title, CamEventListener l) {
        this(null, title, false);
        listener= l;
    }

    void jbInit() throws Exception {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                close();
            }
        });

        panel1.setLayout(borderLayout1);
        jButton1.setText("Close");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        jButton2.setText("<");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton2_actionPerformed(e);
            }
        });
        jButton3.setText(">");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton3_actionPerformed(e);
            }
        });
        jButton4.setText("+");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton4_actionPerformed(e);
            }
        });
        jButton5.setText("-");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton5_actionPerformed(e);
            }
        });
        jButton6.setText("^");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton6_actionPerformed(e);
            }
        });
        jButton7.setText("V");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton7_actionPerformed(e);
            }
        });
        jButton8.setText("Stop");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton8_actionPerformed(e);
            }
        });
        vidwin.setBorder(BorderFactory.createLoweredBevelBorder());
        vidwin.setHorizontalAlignment(SwingConstants.CENTER);
        vidwin.setHorizontalTextPosition(SwingConstants.CENTER);
        getContentPane().add(panel1);
        vidwin.setPreferredSize(new Dimension(640, 480));
        panel1.add(vidwin, BorderLayout.CENTER);
        panel1.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(jButton2, null);
        jPanel1.add(jButton3, null);
        jPanel1.add(jButton4, null);
        jPanel1.add(jButton5, null);
        jPanel1.add(jButton6, null);
        jPanel1.add(jButton7, null);
        jPanel1.add(jButton1, null);
        jPanel1.add(jButton8, null);

        //setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        vidwin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int vw= vidwin.getWidth();
                int vh= vidwin.getHeight();
                Icon ic= vidwin.getIcon();
                if(ic == null) return;
                int w= ic.getIconWidth();
                int h= ic.getIconHeight();
                int x= e.getX()-(vw/2 - w/2);
                int y= e.getY()-(vh/2 - h/2);
                if(x < 0) x= 0; else if(x>w) x= w;
                if(y < 0) y= 0; else if(y>h) y= h;
                int dx= (int)((x*((double)vw/w))-((double)vw/2));
                int dy= (int)((vh/2)-(y*((double)vh/h)));
                System.out.println("camclick at: vidwh " + vw + "," + vh + " x,y " + x + "," + y + " w,h " + w + "," + h + " dx,dy " + dx + "," + dy);
                if(listener != null){
                    listener.handleMove("move:"+dx+","+dy, dx, dy);
                 }
             }
         });
    }

    void setImage(byte [] img){
        ImageIcon ic= new ImageIcon(img);
        vidwin.setIcon(ic);
    }

    void close(){
       if(listener != null) listener.handleClosed();
       dispose();
    }

    // close
    void jButton1_actionPerformed(ActionEvent e) {
        close();
    }

    void jButton2_actionPerformed(ActionEvent e) {
        if(listener != null) listener.handleMove("left:10", -200, 0);
    }

    void jButton3_actionPerformed(ActionEvent e) {
        if(listener != null) listener.handleMove("right:10", +200, 0);
    }

    void jButton4_actionPerformed(ActionEvent e) {
        if(listener != null) listener.handleZoom(true, +20);
    }

    void jButton5_actionPerformed(ActionEvent e) {
       if(listener != null) listener.handleZoom(false, -20);
    }

    void jButton6_actionPerformed(ActionEvent e) {
        if(listener != null) listener.handleMove("up:10", 0, +100);
    }

    void jButton7_actionPerformed(ActionEvent e) {
        if(listener != null) listener.handleMove("down:10", 0, -100);
    }

    void jButton8_actionPerformed(ActionEvent e) {
    	boolean b= jButton8.getText().equalsIgnoreCase("stop");
    	jButton8.setText(b ? "Start" : "Stop");
        if(listener != null) listener.handleStop("stop", b);
    }

}

