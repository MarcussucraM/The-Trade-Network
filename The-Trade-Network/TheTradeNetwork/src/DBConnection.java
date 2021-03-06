import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DBConnection {

	private Connection con;
	private TheMainPanel mainpanel;

	public DBConnection(TheMainPanel mainpanel) {
		this.mainpanel = mainpanel;

		// register with server(dont change this)
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Connects to database
	public void connectToDB() throws SQLException {

		//
		// has to be of form
		// jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]
		//

		// 192.168.254.35 (laptop ip)
		// CONN STRING FOR LAPTOP DB
		String db_connect_string = "jdbc:sqlserver://localhost\\SQLEXPRESS;user=sa;password=password0987654321;databaseName=TheTradeNetworkTest;";

		// String db_connect_string = "jdbc:sqlserver://;servername=localhost;"
		// + "username=garrettmanley;password=111223;database=TheTradeNetwork;";
		con = DriverManager.getConnection(db_connect_string);

	}

	// Queries the database to see if user exists, if so, log in
	public boolean logIn(String username, String pass) throws SQLException {
		String u = username;
		String p = pass;
		// create sql query string(the ?s are variables)
		String SQL = "Select * from PERSON where username = ? and password = ?";

		// create a prepared statement to be executed
		PreparedStatement pstmt = this.con.prepareStatement(SQL);

		// set the ?s to be variables, 1 = first ? etc.
		pstmt.setString(1, u);
		pstmt.setString(2, p);

		// execute query and get a result set(either a 1d or 2d array)
		ResultSet rs = pstmt.executeQuery();

		// if username exists with same pass then log in
		if (rs.next()) {
			return true;
		}

		return false;
	}

	// gets item id from item table for adding items
	public int getItemId(String itemName) throws SQLException {
		String username = mainpanel.getUserName();
		String SQL = "select item.item_id from item where traderA = ? and itemname = ?";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, username);
		pstmt.setString(2, itemName);

		ResultSet rs = pstmt.executeQuery();
		int item_id = -1;
		while (rs.next()) {
			item_id = rs.getInt("item_id");
		}
		return item_id;
	}

	// gets itemid from trade table, makes sure there arent multiple same items
	// from same person
	public int getTradeItemId(String itemName) throws SQLException {
		String username = mainpanel.getUserName();
		String SQL = "select item.item_id from item join tradetable on item.item_id = tradetable.item_id where traderA = ? and itemname = ?";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, username);
		pstmt.setString(2, itemName);

		ResultSet rs = pstmt.executeQuery();
		int item_id = -1;
		while (rs.next()) {
			item_id = rs.getInt("item_id");
		}
		return item_id;
	}

	// adds an item to the database - updates item, tradetable, tradehistory
	public void addItemToDB(String i, String i_d, int c) throws SQLException {
		String username = mainpanel.getUserName();

		// 1.) Add item to item table
		String SQL = "INSERT INTO ITEM VALUES(?,?,?,?)";
		PreparedStatement pstmt = this.con.prepareStatement(SQL);

		pstmt.setString(1, i);
		pstmt.setString(2, i_d);
		pstmt.setInt(3, c);
		pstmt.setString(4, username);
		pstmt.executeUpdate();

		// 2.) Get the Item id of the item you just inserted
		int item_id = getItemId(i);

		// 3.) Insert that item_id into trade and tradehistory tables
		SQL = "insert into tradetable(item_id,offer_id) values(?, null)";
		pstmt = this.con.prepareStatement(SQL);
		pstmt.setInt(1, item_id);
		pstmt.executeUpdate();

		SQL = "insert into tradehistory values(?, null, null)";
		pstmt = this.con.prepareStatement(SQL);
		pstmt.setInt(1, item_id);
		pstmt.executeUpdate();

	}

	public void offerItem(String itemname, String traderA, String itemOffered)
			throws SQLException {
		String user = mainpanel.getUserName();

		int item_id = getTradeItemId(itemOffered);
		System.out.println(item_id);

		// 1.) Insert offer into db
		String SQL = "insert into offer(date_of_offer, traderB, item_id) "
				+ "values(GETDATE(),?,?)";
		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, user);
		pstmt.setInt(2, item_id);
		pstmt.executeUpdate();

		// 2.) Update trade and tradehistory tables
		SQL = "update tradetable "
				+ "set offer_id = (select offer_id from offer where traderB = ? and item_id = ?), "
				+ "offerYN = 'Y' "
				+ "where item_id = (select item_id from item where itemname = ? and traderA = ?)";

		pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, user);
		pstmt.setInt(2, item_id);
		pstmt.setString(3, itemname);
		pstmt.setString(4, traderA);
		pstmt.executeUpdate();

		SQL = "update tradehistory "
				+ "set offer_id = (select offer_id from offer where traderB = ? and item_id = ?) "
				+ "where item_id = (select item_id from item where itemname = ? and traderA = ?)";
		pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, user);
		pstmt.setInt(2, item_id);
		pstmt.setString(3, itemname);
		pstmt.setString(4, traderA);
		pstmt.executeUpdate();

	}

	// get all of your current items
	public String[] getItems() {
		// init variables
		int size = 0;
		String[] items = null;

		System.out.println("stuff");

		// FIRST FIND THE LENGTH OF ARRAY
		try {
			ResultSet rs = con
					.createStatement()
					.executeQuery(
							"Select count(*) AS itemCount from tradetable t join item i on t.item_id = i.item_id where traderA = '"
									+ mainpanel.getUserName() + "'");
			while (rs.next()) {
				size = rs.getInt("itemCount");
			}
			System.out.println(size);

			// AFTER GETTING THE SIZE FILL THE STRING ARRAY WITH ITEMS
			items = new String[size];
			rs = con.createStatement()
					.executeQuery(
							"Select itemName from tradetable t join item i on t.item_id  = i.item_id where traderA = '"
									+ mainpanel.getUserName() + "'");

			for (int i = 0; i < size; i++) {
				rs.next();
				items[i] = rs.getString("itemName");
			}
			return items;

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null,
					"Something went wrong retrieving items");
		}
		return items;
	}

	// gets the categories from db, fills a string[] then sends it to combobox
	// to be filled in AddItemPanel
	public String[] getCategories() {
		// init variables
		int size = 0;
		String[] categories = null;

		// FIRST FIND THE LENGTH OF ARRAY
		try {
			ResultSet rs = con.createStatement().executeQuery(
					"Select count(*) AS categoryCount from category");
			while (rs.next()) {
				size = rs.getInt("categoryCount");
			}

			// AFTER GETTING THE SIZE FILL THE STRING ARRAY WITH CATEGORIES
			categories = new String[size];
			rs = con.createStatement().executeQuery(
					"Select categoryName from category");

			for (int i = 0; i < size; i++) {
				rs.next();
				categories[i] = rs.getString("categoryName");
			}
			return categories;

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null,
					"Something went wrong retrieving categories");
		}
		return categories;
	}

	// Inserts a new user into the database
	public void createUser(String uname, String pass, String zip, String state,
			String phone, String city, String street) throws SQLException {
		String SQL = "INSERT INTO PERSON VALUES(?,?,?,?,?,?,?)";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);

		pstmt.setString(1, uname);
		pstmt.setString(2, pass);
		pstmt.setString(3, zip);
		pstmt.setString(4, state);
		pstmt.setString(5, phone);
		pstmt.setString(6, city);
		pstmt.setString(7, street);

		pstmt.executeUpdate();
	}

	// searches the database for items with a specific name
	public Object[][] searchByItem(String itemname) throws SQLException {
		// 1.)Get number of rows to specify object[][] size
		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"Select count(*) AS itemCount from tradetable t join item i on t.item_id = i.item_id where itemname =  '"
								+ itemname
								+ "' and traderA != '"
								+ mainpanel.getUserName() + "'");
		while (rs.next())
			size = rs.getInt("itemCount");

		// 2.)Define a 2d object array to hold data retrieved
		Object[][] data = new Object[size][4];

		// 3.)Query the db for data wanted
		String SQL = "select itemname, item_description, categoryName, traderA "
				+ "from item join category on item.categoryID = category.categoryID "
				+ "join tradetable t on item.item_id = t.item_id "
				+ "where itemname = ? and traderA != '"
				+ mainpanel.getUserName() + "'";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, itemname);
		rs = pstmt.executeQuery();

		// 4.)Fill data matrix with results
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("itemname");
			data[i][1] = rs.getString("item_description");
			data[i][2] = rs.getString("categoryName");
			data[i][3] = rs.getString("traderA");
			i++;
		}

		return data;
	}

	public Object[][] fillSearchTable() throws SQLException {
		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"Select count(*) AS itemCount from tradetable t join item i on t.item_id = i.item_id "
								+ "where traderA != '"
								+ mainpanel.getUserName() + "'");
		while (rs.next())
			size = rs.getInt("itemCount");

		// 2.)Define a 2d object array to hold data retrieved
		Object[][] data = new Object[size][4];

		// 3.)Query the db for data wanted
		String SQL = "select itemname, item_description, categoryName, traderA "
				+ "from item join category on item.categoryID = category.categoryID "
				+ "join tradetable t on item.item_id = t.item_id "
				+ "where traderA != '" + mainpanel.getUserName() + "'";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		rs = pstmt.executeQuery();

		// 4.)Fill data matrix with results
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("itemname");
			data[i][1] = rs.getString("item_description");
			data[i][2] = rs.getString("categoryName");
			data[i][3] = rs.getString("traderA");
			i++;
		}

		return data;
	}

	// searches the database for items with a specific category
	public Object[][] searchByCategory(String category) throws SQLException {
		// 1.)Get number of rows to specify object[][] size
		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"Select count(*) AS itemCount from item i join category c on i.categoryid = c.categoryid "
								+ "join tradetable t on t.item_id = i.item_id where categoryname ='"
								+ category
								+ "' and traderA != '"
								+ mainpanel.getUserName() + "'");
		while (rs.next())
			size = rs.getInt("itemCount");

		// 2.)Define a 2d object array to hold data retrieved
		Object[][] data = new Object[size][4];

		// 3.)Query the db for data wanted
		String SQL = "select itemname, item_description, categoryName, traderA "
				+ "from item join category on item.categoryID = category.categoryID "
				+ "join tradetable t on item.item_id = t.item_id "
				+ "where categoryName = ? and traderA != '"
				+ mainpanel.getUserName() + "'";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, category);
		rs = pstmt.executeQuery();

		// 4.)Fill data matrix with results
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("itemname");
			data[i][1] = rs.getString("item_description");
			data[i][2] = rs.getString("categoryName");
			data[i][3] = rs.getString("traderA");
			i++;
		}

		return data;
	}

	// searches the database for a specific user selected by current user
	public Object[][] searchByUserName(String uname) throws SQLException {
		// 1.)Get number of rows to specify object[][] size
		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"Select count(*) AS itemCount from item i join tradetable t on i.item_id = t.item_id where traderA = '"
								+ uname
								+ "' and traderA != '"
								+ mainpanel.getUserName() + "'");
		while (rs.next())
			size = rs.getInt("itemCount");

		// 2.)Define a 2d object array to hold data retrieved
		Object[][] data = new Object[size][4];

		// 3.)Query the db for data wanted
		String SQL = "select itemname, item_description, categoryName, traderA "
				+ "from item join category on item.categoryID = category.categoryID "
				+ "join tradetable t on item.item_id = t.item_id "
				+ "where traderA = ? and traderA != '"
				+ mainpanel.getUserName() + "'";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, uname);
		rs = pstmt.executeQuery();

		// 4.)Fill data matrix with results
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("itemname");
			data[i][1] = rs.getString("item_description");
			data[i][2] = rs.getString("categoryName");
			data[i][3] = rs.getString("traderA");
			i++;
		}

		return data;
	}

	// removes an item from the trade table
	public void removeItem(String[] itemValues) throws SQLException {
		String SQL = "delete tradetable where item_id in(select item_id from item where itemname = ? and item_description = ?)";
		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, itemValues[0]);
		pstmt.setString(2, itemValues[1]);
		pstmt.executeUpdate();
	}

	// updates trade history and offer table when user accepts an item
	public void acceptItem(String[] itemValues) throws SQLException {
		String user = mainpanel.getUserName();
		for (int i = 0; i < itemValues.length; i++) {
			System.out.println(itemValues[i]);
		}
		// update offer acceptedYN to yes
		String SQL = "update Offer Set acceptedYN = 'Y' "
				+ "where offer_id = "
				+ "(select o.offer_id "
				+ "from offer o join tradetable t on o.offer_id = t.offer_id "
				+ "join item i on t.item_id = i.item_id "
				+ "where i.item_id in (select item_id from item where traderA = ?) "
				+ "and o.traderB = ? " + "and i.itemname = ?)";

		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, mainpanel.getUserName());
		pstmt.setString(2, itemValues[1]);
		pstmt.setString(3, itemValues[0]);
		pstmt.executeUpdate();

		// update tradehistory accepted date to current date
		SQL = "update tradehistory set date_accepted = GETDATE() "
				+ "where offer_id = "
				+ "(select o.offer_id "
				+ "from offer o join tradetable t on o.offer_id = t.offer_id "
				+ "join item i on t.item_id = i.item_id "
				+ "where i.item_id in (select item_id from item where traderA = ?) "
				+ "and o.traderB = ? " + "and i.itemname = ? "
				+ "and o.acceptedYN = 'Y')";

		pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, mainpanel.getUserName());
		pstmt.setString(2, itemValues[1]);
		pstmt.setString(3, itemValues[0]);
		pstmt.executeUpdate();

	}

	// Query the the db for trade history data
	public Object[][] getTradeHistoryData() throws SQLException {

		// 2d array table data will be stored in
		Object[][] data = null;

		// 1.)figure out how many rows will be needed column size is already
		// known
		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"Select count(*) AS itemCount "
								+ "from item join tradehistory on item.item_id = tradehistory.item_id "
								+ "join offer on tradehistory.offer_id = offer.offer_id "
								+ "join item as itemOffer on itemOffer.item_id = offer.item_id "
								+ "where item.traderA = '"
								+ mainpanel.getUserName() + "' "
								+ "or offer.traderB = '"
								+ mainpanel.getUserName() + "' "
								+ "and tradehistory.date_accepted IS NOT NULL");

		while (rs.next())
			size = rs.getInt("itemCount");

		data = new Object[size][7];

		// 2.) Query the db to get data for trade history table
		// Item traded, Offerer, What they offered, When they offered, If it was
		// accepted
		String SQL = "select item.traderA tA, item.itemname iA, offer.traderB tB, itemOffer.itemname iB, offer.date_of_offer dO, tradehistory.date_accepted aYN "
				+ "from item join tradehistory on item.item_id = tradehistory.item_id "
				+ "join offer on tradehistory.offer_id = offer.offer_id "
				+ "join item as itemOffer on itemOffer.item_id = offer.item_id "
				+ "where item.traderA = ? or offer.traderB = ? "
				+ "and tradehistory.date_accepted IS NOT NULL";
		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, mainpanel.getUserName());
		pstmt.setString(2, mainpanel.getUserName());

		// 3.) Fill 2d array with data needed
		rs = pstmt.executeQuery();
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("tA");
			data[i][1] = rs.getString("iA");
			data[i][2] = rs.getString("tB");
			data[i][3] = rs.getString("iB");
			data[i][4] = rs.getDate("dO");
			data[i][5] = rs.getString("aYN");
			i++;
		}

		return data;
	}

	// Query the db for offer table data
	public Object[][] getOfferTableData() throws SQLException {
		Object[][] data = null;

		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"select count(*) AS itemCount "
								+ "from item i join tradetable t on i.item_id = t.item_id "
								+ "join offer o on t.offer_id = o.offer_id "
								+ "join item itemoffer on o.item_id = itemoffer.item_id "
								+ "where i.traderA = '"
								+ mainpanel.getUserName() + "'");

		while (rs.next()) {
			size = rs.getInt("itemCount");
		}

		data = new Object[size][6];

		String SQL = "select item.itemname iA, offer.traderB tB, itemOffer.itemname iB, offer.date_of_offer dO, offer.acceptedYN aYN "
				+ "from item join tradetable on item.item_id = tradetable.item_id "
				+ "join offer on tradetable.offer_id = offer.offer_id "
				+ "join item as itemOffer on itemOffer.item_id = offer.item_id "
				+ "where item.traderA = ?";
		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, mainpanel.getUserName());

		// 3.) Fill 2d array with data needed
		rs = pstmt.executeQuery();
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("iA");
			data[i][1] = rs.getString("tB");
			data[i][2] = rs.getString("iB");
			data[i][3] = rs.getDate("dO");
			data[i][4] = rs.getString("aYN");
			i++;
		}

		return data;
	}

	// Query the db for trade table data
	public Object[][] getTradeTableData() throws SQLException {
		Object[][] data = null;
		int size = 0;
		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"Select count(*) AS itemCount "
								+ "from tradetable join item on tradetable.item_id = item.item_id "
								+ "join category on item.categoryID = category.categoryID "
								+ "where traderA = '" + mainpanel.getUserName()
								+ "'");

		while (rs.next())
			size = rs.getInt("itemCount");

		data = new Object[size][5];

		String SQL = "select itemname, item_description, categoryName, offerYN "
				+ "from tradetable join item on tradetable.item_id = item.item_id "
				+ "join category on item.categoryID = category.categoryID "
				+ "where traderA = ?";
		PreparedStatement pstmt = this.con.prepareStatement(SQL);
		pstmt.setString(1, mainpanel.getUserName());

		rs = pstmt.executeQuery();
		int i = 0;
		while (rs.next()) {
			data[i][0] = rs.getString("itemname");
			data[i][1] = rs.getString("item_description");
			data[i][2] = rs.getString("categoryName");
			data[i][3] = rs.getString("offerYN");
			i++;
		}
		return data;
	}

	// Query the db for address information between two traders that have
	// accepted trades
	public Object[][] getAcceptedUserInfo() throws SQLException {
		Object[][] data = null;

		ResultSet rs = con
				.createStatement()
				.executeQuery(
						"select count(*) as itemCount "
								+ "from item i join tradetable t on i.item_id = t.item_id "
								+ "join offer o on t.offer_id = o.offer_id "
								+ "join person on o.traderB = person.username "
								+ "join person p2 on i.traderA = p2.username "
								+ "join item offeritem on offeritem.item_id = o.item_id "
								+ "where o.acceptedYN = 'Y' and p2.username = '"
								+ mainpanel.getUserName() + "'");

		int count = 0;

		while (rs.next()) {
			count = rs.getInt("itemCount");
		}

		String currentUser = mainpanel.getUserName();

		rs = con.createStatement()
				.executeQuery(
						"select p2.username as traderA, "
								+ "i.itemname as itemA, p2.phone as phoneA, p2.street as streetA, p2.city as cityA, "
								+ "p2.state as stateA, person.username as TraderB, offeritem.itemname as itemB, "
								+ "person.phone as phoneB, person.street as streetB, person.city as cityB, "
								+ "person.state as stateB, i.item_id as idA, offeritem.item_id as idB "
								+ "from item i join tradetable t on i.item_id = t.item_id "
								+ "join offer o on t.offer_id = o.offer_id "
								+ "join person on o.traderB = person.username "
								+ "join person p2 on i.traderA = p2.username "
								+ "join item offeritem on offeritem.item_id = o.item_id "
								+ "where o.acceptedYN = 'Y' and p2.username = '"
								+ currentUser + "';");

		data = new Object[count][10];

		int i = 0;

		while (rs.next()) {
			data[i][0] = rs.getString("traderA");
			data[i][1] = rs.getString("itemA");
			data[i][2] = rs.getString("traderB");
			data[i][3] = rs.getString("itemB");
			data[i][4] = rs.getString("phoneB");
			data[i][5] = rs.getString("streetB");
			data[i][6] = rs.getString("cityB");
			data[i][7] = rs.getString("stateB");
			data[i][8] = rs.getString("idA");
			data[i][9] = rs.getString("idB");
			i++;
		}

		return data;
	}

	// !!! UPDATE ON GITHUB !!! //completes a trade transaction that has been
	// offered
	public void completeTrade(String idA, String idB) throws SQLException {
		System.out.println("Trade Completed! Trading: " + idA + " For: " + idB);

		String SQL = ("DELETE FROM tradetable WHERE item_id = " + idA + ";");
		con.prepareStatement(SQL).executeUpdate();
		SQL = ("DELETE FROM tradetable WHERE item_id = " + idB + ";");
		con.prepareStatement(SQL).executeUpdate();
	}

}
