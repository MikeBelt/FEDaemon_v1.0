/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fedaemon.produccion.util;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.net.URL;
import javax.swing.ImageIcon;

/**
 *
 * @author DESARROLLO
 */
public class DisplayTrayIcon {
    
    static TrayIcon trayIcon;
    
    public DisplayTrayIcon()
    {

        ShowTrayIcon();
    }
    
    public static void ShowTrayIcon()
    {
    
        if(!SystemTray.isSupported())
        {
            System.out.println("SystemTry no soportado");
            return;
        }
        
        trayIcon=new TrayIcon(CreateIcon("/fedaemon/produccion/img/icono-tevcol-16x16.png","Tray Icon"));
        final SystemTray tray=SystemTray.getSystemTray();
        
        try
        {
        tray.add(trayIcon);
        }
        catch(AWTException e)
        {
            
        }
        
    }
    
    protected static Image CreateIcon(String path ,String desc)
    {
    
        URL imageURL=DisplayTrayIcon.class.getResource(path);
        return (new ImageIcon(imageURL,desc)).getImage();
    }
    
}
