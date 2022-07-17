package com.radicaldevs.vortalbot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

import org.simpleyaml.configuration.file.YamlFile;

import com.radicaldevs.vortalbot.utils.file.yaml.YamlConfigurationFile;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * 
 * The main class of the VortalBot application.
 * 
 * @author Myles Deslippe
 * @since 0.0.1-SNAPSHOT
 *
 */
public class VortalBot {

	/**
	 * The bot's token.
	 */
	private String token;

	/**
	 * The bot's discord api instance.
	 */
	private JDA api;

	/**
	 * The main VortalBot configuration file.
	 */
	private YamlFile config;

	/**
	 * The main VortalBot messages file.
	 */
	private YamlFile messages;

	/**
	 * The plugin directory.
	 */
	private File pluginDirectory;

	/**
	 * The VortalBot singleton instance.
	 */
	private static VortalBot instance;

	/**
	 * The VortalBot logger.
	 */
	private static Logger logger = Logger.getLogger(VortalBot.class.getName());

	/**
	 * The entry point of the program.
	 * 
	 * @param args Arguments passed in through the command line.
	 */
	public static void main(String[] args) {
		// Ensure a token was specified.
		if (args.length == 0) {
			VortalBot.logger.info("Please enter the bot's token as the first command line argument!");
			System.exit(1);
		}

		// Create the instance.
		VortalBot.instance = new VortalBot(args[0]);

		// Load the configuration files, and the plugin directory.
		try {
			VortalBot.getLogger().info("Loading the configuration files...");
			VortalBot.instance.loadFiles();
		} catch (IOException e) {
			VortalBot.getLogger().warning("Could not load the files: " + e.getLocalizedMessage());
			VortalBot.getLogger().info("Shutting down...");
			System.exit(1);
		}

		// Start the bot.
		try {
			VortalBot.getLogger().info("Logging into discord...");
			VortalBot.instance.start();
		} catch (LoginException | InterruptedException e) {
			VortalBot.getLogger().warning("Could not start the bot: " + e.getLocalizedMessage());
			VortalBot.getLogger().info("Shutting down...");
			System.exit(1);
		}

		// Add a shutdown hook.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			VortalBot.getLogger().info("Shutdown command received...");

			if (VortalBot.getInstance() != null && VortalBot.getInstance().api != null)
				VortalBot.getInstance().stop(true);

			VortalBot.getLogger().info("Shutdown complete.");
		}));

		VortalBot.getLogger().info("Bot successfully started!");
	}

	/**
	 * Construct a VortalBot instance.
	 * 
	 * @param token The bot's token.
	 */
	private VortalBot(@Nonnull String token) {
		this.token = token;
	}

	/**
	 * Get the VortalBot's discord API instance.
	 * 
	 * <p>
	 * Note, this will be null if {@link #start()} has not been called.
	 * </p>
	 * 
	 * @return The VortalBot's discord API instance.
	 */
	public JDA getAPI() {
		return this.api;
	}

	/**
	 * Get the bot's configuration file.
	 * 
	 * <p>
	 * Note, this will be null if {@link #loadFiles()} has not been invoked.
	 * </p>
	 * 
	 * @return The bot's configuration file.
	 */
	public YamlFile getConfig() {
		return this.config;
	}

	/**
	 * Get the bot's messages file.
	 * 
	 * <p>
	 * Note, this will be null if {@link #loadFiles()} has not been invoked.
	 * </p>
	 * 
	 * @return The bot's messages file.
	 */
	public YamlFile getMessages() {
		return this.messages;
	}

	/**
	 * The directory containing all of the plugins.
	 * 
	 * @return The plugin directory.
	 */
	public File getPluginDirectory() {
		return this.pluginDirectory;
	}

	/**
	 * Get the VortalBot's singleton instance.
	 * 
	 * @return The VortalBot instance.
	 */
	public static VortalBot getInstance() {
		return VortalBot.instance;
	}

	/**
	 * Get the VortalBot's logger.
	 * 
	 * @return The VortalBot's logger.
	 */
	public static Logger getLogger() {
		return VortalBot.logger;
	}

	/**
	 * Start the bot.
	 * 
	 * @param token The bot's token.
	 * @return The discord api instance.
	 * 
	 * @throws LoginException        If there was an error logging into the bot.
	 * @throws InterruptedException  If the thread is interrupted while establishing
	 *                               a connection with the discord api.
	 * @throws IllegalStateException If the bot is already running.
	 */
	public void start() throws LoginException, InterruptedException {

		// If the bot is already running..
		if (this.api != null)
			throw new IllegalStateException("The bot is already running");

		JDABuilder builder = JDABuilder.createDefault(this.token);
		builder.disableCache(CacheFlag.STICKER, CacheFlag.EMOJI, CacheFlag.MEMBER_OVERRIDES);
		builder.setBulkDeleteSplittingEnabled(false);
		builder.setActivity(Activity.watching(this.config.getString("Activity")));

		this.api = builder.build().awaitReady();
	}

	/**
	 * Stop the bot.
	 * 
	 * @param now If the bot should shutdown immediately, or should process queued
	 *            requests before shutting down.
	 * 
	 * @throws IllegalStateException If the bot is not running.
	 */
	public void stop(boolean now) {
		// If the bot is not running.
		if (this.api == null)
			throw new IllegalStateException("The bot is not running");
		
		if (now)
			this.api.shutdownNow();
		else
			this.api.shutdown();

		this.api = null;
		this.config = null;
		this.messages = null;
	}

	/**
	 * Ensure the main configuration files exist, and load them.
	 * 
	 * @throws IOException If there was an issue generating, or reading the
	 *                     configuration files.
	 */
	private void loadFiles() throws IOException {
		this.pluginDirectory = new File("plugins");

		if (!this.pluginDirectory.exists()) {
			this.pluginDirectory.mkdir();
		}

		YamlConfigurationFile config = new YamlConfigurationFile("./config.yml");
		config.setDefaults(this.getInternalResource("defaults/files/config.yml"));
		config.createIfNotExists();
		config.load();

		this.config = config.getConfiguration();

		YamlConfigurationFile messages = new YamlConfigurationFile("./messages.yml");
		messages.setDefaults(this.getInternalResource("defaults/files/messages.yml"));
		messages.createIfNotExists();
		messages.load();

		this.messages = messages.getConfiguration();
	}

	/**
	 * Get an internal resource.
	 * 
	 * <p>
	 * Note: This will return null if the specified resource does not exist.
	 * </p>
	 * 
	 * @param path The path to the resource.
	 * 
	 * @return The resource if it exists, otherwise null.
	 */
	private File getInternalResource(String path) {
		path = "resources/" + path;

		try {
			File file = File.createTempFile(path.split("\\.")[0], path.split("\\.")[1]);
			Files.copy(VortalBot.class.getClassLoader().getResourceAsStream(path), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

			return file;
		} catch (Exception e) {
			return null;
		}
	}

}
