/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package apidemo;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import apidemo.util.HtmlButton;
import apidemo.util.NewTabbedPanel;
import apidemo.util.NewTabbedPanel.INewTab;
import apidemo.util.TCombo;
import apidemo.util.VerticalPanel;

import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.IFundamentalsHandler;
import com.ib.controller.NewContract;
import com.ib.controller.NewContractDetails;
import com.ib.controller.Types.FundamentalType;

import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ContractInfoPanel extends JPanel {
	private final NewContract m_contract = new NewContract();
	private final NewTabbedPanel m_requestPanels = new NewTabbedPanel();
	private final NewTabbedPanel m_resultsPanels = new NewTabbedPanel();

	public File m_testInFile = new File("/home/samb1/dev/IBJts/twsjavas/gbpftsetickers.txt");
	public File m_testoutFile = new File("test_out_file.txt");
	public File m_contractsoutFile = new File("contracts_out_file.txt");

	public FileWriter m_writertest;
	public FileWriter m_contractwriter;

	public FileInputStream m_fis;
	public BufferedReader m_buffreadr;

	public int m_iFileCounter = 0;


	ContractInfoPanel() {
		m_requestPanels.addTab( "Contract details", new DetailsRequestPanel() );
		m_requestPanels.addTab( "Fundamentals", new FundaRequestPanel() );

		setLayout( new BorderLayout() );
		add( m_requestPanels, BorderLayout.NORTH);
		add( m_resultsPanels);

		//adding stuff to set up files
		try {
			m_testoutFile.createNewFile();
			m_writertest = new FileWriter(m_testoutFile);
			m_fis = new FileInputStream(m_testInFile);
			m_buffreadr = new BufferedReader(new InputStreamReader(m_fis));
		} catch ( Exception e) {
			System.out.println(e.getMessage());
		}

	}

	class DetailsRequestPanel extends JPanel {
		ContractPanel m_contractPanel = new ContractPanel( m_contract);

		DetailsRequestPanel() {
			HtmlButton but = new HtmlButton( "Query") {
				@Override protected void actionPerformed() {
					onQuery();
				}
			};

			setLayout( new BoxLayout( this, BoxLayout.X_AXIS) );
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20));
			add( but);
		}

		protected void onQuery() {
			m_contractPanel.onOK();

			DetailsResultsPanel panel = new DetailsResultsPanel();
			m_resultsPanels.addTab( m_contract.symbol() + " " + "Description", panel, true, true);
			ApiDemo.INSTANCE.controller().reqContractDetails(m_contract, panel);
		}
	}

	class DetailsResultsPanel extends JPanel implements IContractDetailsHandler {
		JLabel m_label = new JLabel();
		JTextArea m_text = new JTextArea();

		DetailsResultsPanel() {
			JScrollPane scroll = new JScrollPane( m_text);

			setLayout( new BorderLayout() );
			add( m_label, BorderLayout.NORTH);
			add( scroll);
		}

		@Override public void contractDetails(ArrayList<NewContractDetails> list) {
			// set label
			if (list.size() == 0) {
				m_label.setText( "No matching contracts were found");
			} else if (list.size() > 1) {
				m_label.setText( list.size() + " contracts returned; showing first contract only");
			} else {
				m_label.setText( null);
			}

			// set text
			if (list.size() == 0) {
				m_text.setText( null);
			} else {
				m_text.setText( list.get( 0).toString() );
			}
		}
	}

	public class FundaRequestPanel extends JPanel {
		ContractPanel m_contractPanel = new ContractPanel( m_contract);
		TCombo<FundamentalType> m_type = new TCombo<FundamentalType>( FundamentalType.values() );

		FundaRequestPanel() {
			HtmlButton but = new HtmlButton( "Query") {
				@Override protected void actionPerformed() {
					onQuery();
				}
			};

			VerticalPanel rightPanel = new VerticalPanel();
			rightPanel.add( "Report type", m_type);

			setLayout( new BoxLayout( this, BoxLayout.X_AXIS));
			add( m_contractPanel);
			add( Box.createHorizontalStrut(20));
			add( rightPanel);
			add( Box.createHorizontalStrut(10));
			add( but);
		}

		protected void onQuery() {
			m_contractPanel.onOK();

			String line = null;
			try {
				while ( (line = m_buffreadr.readLine()) != null) {
					System.out.println(line);
					TimeUnit.MILLISECONDS.sleep(200);

					m_contract.symbol(line);

					FundaResultPanel panel = new FundaResultPanel();
					FundamentalType type = m_type.getSelectedItem();
					//m_resultsPanels.addTab( m_contract.symbol() + " " + type, panel, true, true);
					ApiDemo.INSTANCE.controller().reqFundamentals( m_contract, type, panel);

				}
			} catch ( Exception e) {
				System.out.println(e.getMessage());
			}


		}
	}

	class FundaResultPanel extends JPanel implements INewTab, IFundamentalsHandler {
		String m_data;
		JTextArea m_text = new JTextArea();

		FundaResultPanel() {
			HtmlButton b = new HtmlButton( "View in browser") {
				@Override protected void actionPerformed() {
					onView();
				}
			};

			JScrollPane scroll = new JScrollPane( m_text);
			setLayout( new BorderLayout() );
			add( scroll);
			add( b, BorderLayout.EAST);
		}

		protected void onView() {
			try {
				File file = File.createTempFile( "tws", ".xml");
				FileWriter writer = new FileWriter( file);
				writer.write( m_text.getText() );
				writer.flush();
				writer.close();
				Desktop.getDesktop().open( file);
			} catch ( Exception e) {
				e.printStackTrace();
			}
		}

		/** Called when the tab is first visited. */
		@Override public void activated() {
			ApiDemo.INSTANCE.controller().reqFundamentals(m_contract, FundamentalType.ReportRatios, this);
		}

		/** Called when the tab is closed by clicking the X. */
		@Override public void closed() {
		}

		@Override public void fundamentals(String str) {
			m_data = str;
			m_text.setText( str);

			System.out.println(str);
			System.out.println(m_iFileCounter);
			m_iFileCounter++;
			//String outFileName = "outfile" + m_iFileCounter;
			String outFileName = "xmltestouts" + File.separator + "arsesfile" + m_iFileCounter + ".xml";
			try {
				File outFile = new File(outFileName);
				FileWriter owriter = new FileWriter(outFile);
                                owriter.write(str);
                                owriter.flush();
                                owriter.close();
			} catch ( Exception e) {
				System.out.println(e.getMessage());
			}



		}
	}
}
