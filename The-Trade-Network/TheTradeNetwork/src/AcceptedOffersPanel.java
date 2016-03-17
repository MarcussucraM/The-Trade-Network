import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.Action;

public class AcceptedOffersPanel extends JPanel implements StringConstants {
	private JTable table;
	private ProjectTableModel model;
	private JTabbedPane tabbedPane;
	protected TheMainPanel mainpanel;
	private JButton btnCompleteTrade;
	private final Action action = new SwingAction();
	private DBConnection db;
	private final Action action_1 = new SwingAction_1();
	private int tabCount = 1;
	private Object[][] data;

	// protected Object[][]

	/**
	 * Create the panel.
	 */
	public AcceptedOffersPanel(TheMainPanel mainpanel, DBConnection db) {
		setLayout(null);

		this.mainpanel = mainpanel;
		this.db = db;

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(10, 10, 770, 450);
		add(tabbedPane);

		JButton btnGoBack = new JButton("GO BACK");
		btnGoBack.setAction(action);
		btnGoBack.setBounds(471, 500, 89, 40);
		add(btnGoBack);

		btnCompleteTrade = new JButton("Complete Trade");
		btnCompleteTrade.setAction(action_1);
		btnCompleteTrade.setBounds(150, 500, 150, 40);
		add(btnCompleteTrade);

		createTables();

	}
	
	public void refreshPage(){
		try {
			model.refresh(offerInformationColumns,
						db.getAcceptedUserInfo());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createTables() {
		try {
			model = new ProjectTableModel(offerInformationColumns,
					db.getAcceptedUserInfo());
			table = new JTable(model);
			if (tabCount == 1) {
				tabbedPane.addTab("TEST", null, new JScrollPane(table), null);
				tabCount++;
			}
		} catch (Exception e) {
			System.err.println("SOMETHING FUCKED UP");
			e.printStackTrace();
		}
	}

	private class SwingAction extends AbstractAction {
		public SwingAction() {
			putValue(NAME, "GO BACK");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}

		public void actionPerformed(ActionEvent e) {
			CardLayout cl = (CardLayout) (mainpanel.getLayout());
			cl.show(mainpanel, MYTRADES);
			try {
				mainpanel.getMyTradesPage().refresh_page();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private class SwingAction_1 extends AbstractAction {
		public SwingAction_1() {
			putValue(NAME, "COMPLETE TRADE");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}

		public void actionPerformed(ActionEvent e) {
			String idA, idB;
			int row = table.getSelectedRow();
			if (row != -1) {
				try {
					data = db.getAcceptedUserInfo();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				idA = (String) data[row][8];
				idB = (String) data[row][9];
				if (JOptionPane
						.showConfirmDialog(
								mainpanel,
								"Are you sure you want to\nComplete this Trade?\nNote: Item will be removed from tradetable if you accept.\n "
										+ data[row][1] + " for " + data[row][3]) == JOptionPane.YES_OPTION)
					try {
						db.completeTrade(idA, idB);
						model.refresh(offerInformationColumns,
								db.getAcceptedUserInfo());
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			} else {
				JOptionPane.showMessageDialog(mainpanel,
						"You need to select a row first!");
			}
		}
	}
}