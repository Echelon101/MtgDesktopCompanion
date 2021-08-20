package org.magic.api.dao.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Map;

import org.magic.api.interfaces.abstracts.AbstractMagicSQLDAO;

public class MysqlDAO extends AbstractMagicSQLDAO {

	private static final String MYSQL_DUMP_PATH = "MYSQL_DUMP_PATH";

	@Override
	protected String getAutoIncrementKeyWord() {
		return "INTEGER AUTO_INCREMENT";
	}
	

	@Override
	protected String getjdbcnamedb() {
		return getName().toLowerCase();
	}

	@Override
	protected String beanStorage() {
		return "LONGTEXT";
	}

	@Override
	protected String longTextStorage() {
		return "LONGTEXT";
	}

	
	public String getdbSizeQuery() {
		return "SELECT Round(Sum(data_length + index_length), 1) FROM information_schema.tables WHERE  table_schema = '"+getString(DB_NAME)+"'";
	}


	@Override
	public String getName() {
		return "MySQL";
	}
	
	@Override
	public void backup(File f) throws SQLException, IOException {

		if (getString(MYSQL_DUMP_PATH).length() <= 0)
			throw new NullPointerException("Please fill MYSQL_DUMP_PATH var");

		if (!getFile(MYSQL_DUMP_PATH).exists())
			throw new IOException(getString(MYSQL_DUMP_PATH) + " doesn't exist");

		var dumpCommand = new StringBuilder();
		dumpCommand.append(getString(MYSQL_DUMP_PATH)).append("/mysqldump ").append(getString(DB_NAME))
				   .append(" -h ").append(getString(SERVERNAME))
				   .append(" -u ").append(getString(LOGIN))
				   .append(" -p").append(getString(PASS))
				   .append(" --port ").append(getString(SERVERPORT));
		
		var rt = Runtime.getRuntime();
		logger.info("begin Backup " + getString(DB_NAME));
		Process child;

		child = rt.exec(dumpCommand.toString());
		try (var ps = new PrintStream(f)) {
			var in = child.getInputStream();
			int ch;
			while ((ch = in.read()) != -1) {
				ps.write(ch);
			}
			logger.info("Backup " + getString(DB_NAME) + " done");
		}

	}


	@Override
	public Map<String, String> getDefaultAttributes() {
		return Map.of(SERVERPORT, "3306",
				   LOGIN, "mariadb",
				   PASS, "mariadb",
				   PARAMS, "?autoDeserialize=true&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true",
				   MYSQL_DUMP_PATH, "C:\\Program Files (x86)\\Mysql\\bin"
	);
	}
	


	@Override
	public String createListStockSQL() {
		return "select * from stocks where collection=? and JSON_EXTRACT(mcard,'$.name')=?";
	}


}