package org.magic.gui;

import javax.swing.JWindow;

import org.magic.services.MTGControler;
import org.magic.services.ThreadManager;

import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.ImageIcon;

public class LaunchWindows extends JWindow {
	
	JProgressBar progressBar;
	
	public void start()
	{
		setVisible(true);
		progressBar.setValue(0);
	}
	
	public LaunchWindows() {
		
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(true);
		
		getContentPane().add(progressBar, BorderLayout.SOUTH);
		//setOpacity(0.55f);
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setIcon(new ImageIcon(LaunchWindows.class.getResource("/res/data/magic-logo.png")));
		lblNewLabel.setOpaque(false);
		getContentPane().add(lblNewLabel, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
	}

	public void update(Object msg) {
		progressBar.setString(msg.toString());
	}

	public void stop() {
		setVisible(false);
		dispose();
	}


}
