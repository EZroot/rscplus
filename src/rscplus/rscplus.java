/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package rscplus;

import Game.Reflection;
import Game.Renderer;
import java.applet.Applet;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Graphics;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JFrame;

public class rscplus extends JApplet implements ComponentListener, WindowListener
{
	public void initializeInstance(boolean applet)
	{
		m_jconfig = new JConfig();
		m_jclassloader = new JClassLoader();
		m_appletstub = new RSC_AppletStub();

		getContentPane().setBackground(Color.BLACK);
		getContentPane().setPreferredSize(new Dimension(512, 346));
		addComponentListener(this);

		m_isApplet = applet;
	}

	public static rscplus getInstance()
	{
		return m_instance;
	}

	public JConfig getJConfig()
	{
		return m_jconfig;
	}

	public JClassLoader getJClassLoader()
	{
		return m_jclassloader;
	}

	public Applet getApplet()
	{
		return m_applet;
	}

	public boolean isApplet()
	{
		return m_isApplet;
	}

	public boolean load()
	{
		Logger.Info("Loading rscplus config...");
		Settings.Load();

		Logger.Info("Loading JConfig...");
		if(!m_jconfig.fetch(Util.MakeWorldURL(Settings.WORLD)))
		{
			Logger.Error("Unable to fetch JConfig");
			return false;
		}

		Logger.Info("Checking if JConfig is supported...");
		if(!m_jconfig.isSupported())
		{
			Logger.Error("JConfig loader is outdated, please wait for an update");
			return false;
		}

		Logger.Info("Loading rsc JAR...");
		if(!m_jclassloader.fetch(m_jconfig.getJarURL()))
		{
			Logger.Error("JClassLoader is unable to obtain the rsc JAR");
			return false;
		}

		Logger.Info("Creating rsc instance...");
		try
		{
			Class<?> client = m_jclassloader.loadClass(m_jconfig.getJarClass());
			m_applet = (Applet)client.newInstance();
		}
		catch(Exception e)
		{
			Logger.Error("Unable to create rsc instance");
			e.printStackTrace();
			return false;
		}

		Logger.Info("Setting rsc client applet stub...");
		m_applet.setStub(m_appletstub);

		Logger.Info("Adding applet to JApplet...");
		add(m_applet);
		revalidate();

		Logger.Info("Client loaded successfully");

		return true;
	}

	public void run()
	{
		if(m_applet == null)
			return;

		Logger.Info("Finding reflection hooks...");
		Reflection.Load();

		Logger.Info("Initializing renderer...");
		Renderer.init();

		Logger.Info("Running rsc code, have fun :)");
		m_applet.init();
		m_applet.start();
	}

	/*
	 *	Applet entry points
	 */
	@Override
	public void start()
	{
		Logger.Info("Starting rscplus in applet mode");

		// Create rscplus instance
		m_instance = this;
		m_instance.initializeInstance(true);

		// Load rsc client
		if(!m_instance.load())
		{
			// TODO: Is this safe to call in an applet?
			System.exit(0);
			return;
		}

		// Run the client
		m_instance.run();
	}

	/*
	 *	Applet exit point
	 */
	@Override
	public void stop()
	{
		if(m_applet == null)
			return;

		m_applet.stop();
		m_applet.destroy();
	}

	/*
	 *	Application entry point
	 */
	public static void main(String args[])
	{
		Logger.Info("Starting rscplus in application mode");

		// Create rscplus instance
		m_instance = new rscplus();
		m_instance.initializeInstance(false);

		// Create rscplus window
		m_jframe = new JFrame();
		m_jframe.addWindowListener(m_instance);
		m_jframe.setResizable(true);
		m_jframe.setTitle("rscplus");
		m_jframe.setContentPane(m_instance);
		m_jframe.pack();
		m_jframe.setLocationRelativeTo(null);

		// Set window icon
		URL iconURL = Settings.getResource("/assets/icon.png");
		if(iconURL != null)
		{
			ImageIcon icon = new ImageIcon(iconURL);
			m_jframe.setIconImage(icon.getImage());
		}

		m_jframe.setVisible(true);

		// Load rsc client
		if(!m_instance.load())
		{
			System.exit(0);
			return;
		}

		// Run the client
		m_instance.run();
	}

	@Override
	public final void windowClosed(WindowEvent e)
	{
		stop();
	}

	@Override
	public final void windowClosing(WindowEvent e)
	{
		m_jframe.dispose();
	}

	@Override
	public final void windowOpened(WindowEvent e)
	{
	}

	@Override
	public final void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public final void windowActivated(WindowEvent e)
	{
	}

	@Override
	public final void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public final void windowIconified(WindowEvent e)
	{
	}

	@Override
	public final void componentHidden(ComponentEvent e)
	{
	}

	@Override
	public final void componentMoved(ComponentEvent e)
	{
	}

	@Override
	public final void componentResized(ComponentEvent e)
	{
		Dimension size = new Dimension(getContentPane().getWidth(), getContentPane().getHeight());

		Logger.Debug("Resized to " + size.width + "x" + size.height);

		if(m_applet != null)
			Renderer.resize(size.width, size.height);
	}

	@Override
	public final void componentShown(ComponentEvent e)
	{
	}

	private JConfig m_jconfig;
	private JClassLoader m_jclassloader;
	private RSC_AppletStub m_appletstub;
	private Applet m_applet = null;
	private boolean m_isApplet;

	// This is only used in application mode
	private static JFrame m_jframe;

	// Singleton
	private static rscplus m_instance = null;
}
