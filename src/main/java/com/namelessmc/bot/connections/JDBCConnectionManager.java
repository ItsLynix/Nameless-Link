package com.namelessmc.bot.connections;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;

public abstract class JDBCConnectionManager extends ConnectionManager {

	public abstract Connection getNewDatabaseConnection() throws SQLException;

	@Override
	public Optional<NamelessAPI> getApi(final long guildId) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			String apiUrl;
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT api_url FROM connections WHERE guild_id=?")) {
				statement.setLong(1, guildId);
				final ResultSet result = statement.executeQuery();
				if (!result.next()) {
					return Optional.empty();
				}
				apiUrl = result.getString(1);
			}

			try (PreparedStatement statement = connection
					.prepareStatement("UPDATE connections SET last_use=? WHERE guild_id=?")) {
				statement.setLong(1, System.currentTimeMillis());
				statement.setLong(2, guildId);
				statement.executeUpdate();
			}

			return Optional.of(Main.newApiConnection(new URL(apiUrl)));
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		} catch (final MalformedURLException e) {
			// This should never happen since malformed URLs are not allowed in the database
			// Pretend as if the website was not set up
			e.printStackTrace();
			return Optional.empty();
		}
	}

	@Override
	public void newConnection(final long guildId, final URL apiUrl) throws BackendStorageException {
		Validate.notNull(apiUrl, "Api url is null");
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO connections (guild_id, api_url, last_use) VALUES (?, ?, ?)")) {
				statement.setLong(1, guildId);
				statement.setString(2, apiUrl.toString());
				statement.setLong(3, System.currentTimeMillis());
				statement.execute();
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public boolean updateConnection(final long guildId, final URL apiUrl) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("UPDATE connections SET api_url=? WHERE guild_id=?")) {
				statement.setString(1, apiUrl.toString());
				statement.setLong(2, guildId);
				return statement.executeUpdate() > 0;
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public boolean removeConnection(final long guildId) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("DELETE FROM connections WHERE guild_id=?")) {
				statement.setLong(1, guildId);
				return statement.execute();
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public List<URL> listConnections() throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("SELECT api_url FROM connections")) {
				final ResultSet result = statement.executeQuery();
				final List<URL> urls = new ArrayList<>();
				while (result.next()) {
					try {
						urls.add(new URL(result.getString("api_url")));
					} catch (final MalformedURLException e) {
						System.err.println("Skipped invalid URL in listConnections(): " + result.getString("api_url"));
						e.printStackTrace();
					}
				}
				return urls;
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public Optional<Long> getLastUsed(final long guildId) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT api_url FROM connections WHERE guild_id=?")) {
				statement.setLong(1, guildId);
				final ResultSet result = statement.executeQuery();
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(result.getLong(1));
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

}