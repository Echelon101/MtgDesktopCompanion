package org.magic.gui.components.dialog;

import static org.magic.tools.MTG.getEnabledPlugin;
import static org.magic.tools.MTG.capitalize;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.apache.log4j.Logger;
import org.magic.api.beans.MTGNotification;
import org.magic.api.beans.MTGNotification.MESSAGE_TYPE;
import org.magic.api.beans.MagicCard;
import org.magic.api.beans.MagicCollection;
import org.magic.api.beans.MagicDeck;
import org.magic.api.beans.MagicEdition;
import org.magic.api.interfaces.MTGCardsProvider;
import org.magic.gui.abstracts.AbstractBuzyIndicatorComponent;
import org.magic.services.MTGConstants;
import org.magic.services.MTGControler;
import org.magic.services.MTGLogger;
import org.magic.services.threads.ThreadManager;
import org.magic.services.workers.AbstractObservableWorker;
import org.magic.tools.UITools;

public class MassCollectionImporterDialog extends JDialog {

	private static final String NAME = "name";
	private static final String NUMBER = "number";
	private static final long serialVersionUID = 1L;
	private String[] ids;
	private MagicDeck deck;
	private transient Logger logger = MTGLogger.getLogger(this.getClass());
	private JComboBox<MagicEdition> cboEditions;
	private JComboBox<String> cboByType;
	

	public MassCollectionImporterDialog() {
		setIconImage(MTGConstants.ICON_MASS_IMPORT.getImage());
		setTitle(capitalize("MASS_CARDS_IMPORT"));
		try {
			initGUI();
		} catch (Exception e) {
			logger.error("error init", e);
		}
	}

	private List<MagicCard> ids(List<String> without)
	{
		List<MagicCard> ret = new ArrayList<>();
		try {
			ret = new ArrayList<>(getEnabledPlugin(MTGCardsProvider.class).searchCardByEdition((MagicEdition)cboEditions.getSelectedItem()));
			
			if(cboByType.getSelectedItem().equals(NUMBER))
				ret.removeIf(ca->without.contains(ca.getCurrentSet().getNumber()));
			else
				ret.removeIf(ca->without.contains(ca.getName()));
			
			return ret;
			
		} catch (IOException e) {
			return ret;
		}
	}
	
	
	private void initGUI() {
		getContentPane().setLayout(new BorderLayout(0, 0));
		deck = new MagicDeck();
		JPanel panelCollectionInput = new JPanel();
		getContentPane().add(panelCollectionInput, BorderLayout.NORTH);

		JLabel lblImport = new JLabel(capitalize("IMPORT") + " ");
		panelCollectionInput.add(lblImport);
		
		
		cboEditions =UITools.createComboboxEditions();
		panelCollectionInput.add(cboEditions);
		
		JLabel lblNewLabel = new JLabel(capitalize("BY"));
		panelCollectionInput.add(lblNewLabel);

		cboByType = UITools.createCombobox(new String[] { NUMBER, NAME });
		panelCollectionInput.add(cboByType);

		JLabel lblIn = new JLabel("in");
		panelCollectionInput.add(lblIn);
		
		JComboBox<MagicCollection> cboCollections = UITools.createComboboxCollection();
		panelCollectionInput.add(cboCollections);

		JPanel panneauBas = new JPanel();
		getContentPane().add(panneauBas, BorderLayout.SOUTH);
		AbstractBuzyIndicatorComponent progressBar = AbstractBuzyIndicatorComponent.createProgressComponent();
		

		final JCheckBox checkNewOne = new JCheckBox(capitalize("IMPORT_OTHER_SERIE"));

		JButton btnInverse = new JButton("Inverse");
		
		JButton btnImport = new JButton(capitalize("IMPORT"),MTGConstants.ICON_MASS_IMPORT);

		panneauBas.add(btnInverse);

		panneauBas.add(checkNewOne);

		panneauBas.add(btnImport);

		panneauBas.add(progressBar);

		JTextPane txtNumbersInput = new JTextPane();
		
		txtNumbersInput.setPreferredSize(new Dimension(600, 300));
		

		getContentPane().add(new JScrollPane(txtNumbersInput), BorderLayout.CENTER);


		setModal(true);
		setLocationRelativeTo(null);
		
		pack();

		btnInverse.addActionListener(e -> {
			List<String> elements = Arrays.asList(txtNumbersInput.getText().replace("\n", " ").replace("  ", " ").trim().split(" "));
			StringBuilder temp = new StringBuilder();
			for (MagicCard s : ids(elements))
				temp.append(s.getCurrentSet().getNumber()).append(" ");

			txtNumbersInput.setText(temp.toString());
		});
		

		
		
		btnImport.addActionListener(e -> {
			final MagicEdition ed = (MagicEdition) cboEditions.getSelectedItem();
			final MagicCollection col = (MagicCollection) cboCollections.getSelectedItem();

			if (cboByType.getSelectedItem().equals(NUMBER))
				ids = txtNumbersInput.getText().replace("\n", " ").replace("  ", " ").trim().split(" ");
			else
				ids = txtNumbersInput.getText().split("\n");
		
			AbstractObservableWorker<Void, MagicCard, MTGCardsProvider> sw = new AbstractObservableWorker<>(progressBar,getEnabledPlugin(MTGCardsProvider.class),ids.length) {

				@Override
				protected void notifyEnd() {
					MTGControler.getInstance().notify(new MTGNotification(
							capitalize("FINISHED"),
							MTGControler.getInstance().getLangService().getCapitalize("X_ITEMS_IMPORTED", ids.length),
							MESSAGE_TYPE.INFO
							));
				}
				
				

				@Override
				protected void done() {
					super.done();
					if (!checkNewOne.isSelected()) {
						dispose();
					}
				}



				@Override
				protected Void doInBackground() throws Exception {
					for (String id : ids) {
						try {
							MagicCard mc = null;

							if (cboByType.getSelectedItem().toString().equalsIgnoreCase(NUMBER))
								mc = plug.getCardByNumber(id.trim(), ed);
							else
								mc = plug.searchCardByName( id.replace("\n", " ").replace("  ", " ").trim(),
												(MagicEdition) cboEditions.getSelectedItem(), true)
										.get(0);

							deck.add(mc);
							MTGControler.getInstance().saveCard(mc,col,null);
							
						} catch (Exception e1) {
							logger.error(e1);
						}
					}
					
					return null;
				}
			};

			ThreadManager.getInstance().runInEdt(sw, "btnImport importCards");
		});

	}

	public MagicDeck getAsDeck() {
		return deck;
	}

	public void setDefaultEdition(MagicEdition magicEdition) {
		cboEditions.setSelectedItem(magicEdition);
		
	}

}
