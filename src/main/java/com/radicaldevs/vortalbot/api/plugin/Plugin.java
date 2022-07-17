package com.radicaldevs.vortalbot.api.plugin;

/**
 * 
 * A base-plugin class that can be implemented to created plugins that interface
 * with the bot at runtime.
 * 
 * @author Myles Deslippe
 * @since 0.0.3-SNAPSHOT
 *
 */
public abstract class Plugin {

	/**
	 * A method that is invoked when the plugin is enabled.
	 */
	public abstract void onEnable();

	/**
	 * A method that is invoked when the lpugin is disabled.
	 */
	public abstract void onDisable();

}
