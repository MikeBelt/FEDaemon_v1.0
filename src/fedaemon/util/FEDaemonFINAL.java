

package fedaemon.util;

import fedaemon.frms.frmConexionBD;
import java.io.File;
import java.lang.management.ManagementFactory;

/**
 *
 * @author Mike
 */
public final class FEDaemonFINAL {

    /**
     * @param args the command line arguments
     */
    
    private static Empresa empresaTevcol=null;
    private static Empresa empresaTevsur=null;
    private static Servicio servicioTevcol=null;
    private static Servicio servicioTevsur=null;
    private static frmConexionBD frmConexionTevcol=null;
    private static frmConexionBD frmConexionTevsur=null;
    
    
    public static void main(String[] args) {
        
        empresaTevcol=new Empresa();
        empresaTevcol.setNombre("TEVCOL");
        empresaTevcol.setServidor("192.168.1.18");
        empresaTevcol.setBase("GLTEVCOL");
        empresaTevcol.setUsuario("SISTEMAS");
        empresaTevcol.setPasword("SISTEMAS2015FE");
        
        servicioTevcol=new Servicio();
        servicioTevcol.setAmbiente("2");
        servicioTevcol.setVersion("2015.09.28");
        servicioTevcol.setSo(System.getProperty("os.name"));
        servicioTevcol.setArquitectura(System.getProperty("os.arch"));
        servicioTevcol.setPid(pid());
        
        crearDirectoriosTevcol();
        
        frmConexionTevcol=new frmConexionBD();
        frmConexionTevcol.setEmpresa(empresaTevcol);
        frmConexionTevcol.setServicio(servicioTevcol);
        frmConexionTevcol.ejecutarForm();
        
        empresaTevsur=new Empresa();
        empresaTevsur.setNombre("TEVSUR");
        empresaTevsur.setServidor("192.168.15.250");
        empresaTevsur.setBase("GLTEVSUR");
        empresaTevsur.setUsuario("SISTEMAS");
        empresaTevsur.setPasword("TEVSUR");
        
        servicioTevsur=new Servicio();
        servicioTevsur.setAmbiente("2");
        servicioTevsur.setVersion("2015.09.28");
        servicioTevsur.setSo(System.getProperty("os.name"));
        servicioTevsur.setArquitectura(System.getProperty("os.arch"));
        servicioTevsur.setPid(pid());
        
        crearDirectoriosTevsur();
        
        frmConexionTevsur=new frmConexionBD();
        frmConexionTevsur.setEmpresa(empresaTevsur);
        frmConexionTevsur.setServicio(servicioTevsur);
        frmConexionTevsur.ejecutarForm();
        
        
    }
    
    public static void crearDirectoriosTevcol(){
        //Sistema Operativo
        System.out.println("Sistema Operativo: " + System.getProperty("os.name"));
        //Arquitectura
        System.out.println("Sobre arquitectura: " + System.getProperty("os.arch"));
        //Version  
        System.out.println("Versión " + System.getProperty("os.version"));
    
        if(System.getProperty("os.name").contains("Windows"))
        {
            String carpeta = "c:\\FEDaemonTEVCOL";
            File acceso = new File(carpeta);
                        
            if (!acceso.exists()) 
                acceso.mkdir();
            servicioTevcol.setDirectorioRaiz(carpeta);
            
            carpeta = "c:\\FEDaemonTEVCOL\\Facturas";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioFacturas(carpeta+"\\");
            
            
            carpeta = "c:\\FEDaemonTEVCOL\\Retenciones";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioRetenciones(carpeta+"\\");
            
            carpeta = "c:\\FEDaemonTEVCOL\\NC";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioNotasCredito(carpeta+"\\");
            
            carpeta = "c:\\FEDaemonTEVCOL\\ND";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioNotasDebito(carpeta+"\\");
            
            
        }
        if(System.getProperty("os.name").contains("Linux"))
        {
            System.out.println("PID del proceso: " + pid());
            
            
            String carpeta = "FEDaemonTEVCOL";
            File acceso = new File(carpeta);
            
            if (!acceso.exists()) 
                acceso.mkdir();
            servicioTevcol.setDirectorioRaiz(carpeta);
            
            carpeta = "FEDaemonTEVCOL/Facturas";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioFacturas(carpeta+"/");
            
            carpeta = "FEDaemonTEVCOL/Retenciones";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioRetenciones(carpeta+"/");
            
            carpeta = "FEDaemonTEVCOL/NC";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioNotasCredito(carpeta+"/");
            
            carpeta = "FEDaemonTEVCOL/ND";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevcol.setDirectorioNotasDebito(carpeta+"/");

        }
    }
    
    
    private static void crearDirectoriosTevsur(){
    //Sistema Operativo
        System.out.println("Sistema Operativo: " + System.getProperty("os.name"));
        //Arquitectura
        System.out.println("Sobre arquitectura: " + System.getProperty("os.arch"));
        //Version  
        System.out.println("Versión " + System.getProperty("os.version"));
    
        if(System.getProperty("os.name").contains("Windows"))
        {
            String carpeta = "c:\\FEDaemonTEVSUR";
            File acceso = new File(carpeta);
                        
            if (!acceso.exists()) 
                acceso.mkdir();
            servicioTevsur.setDirectorioRaiz(carpeta);
            
            
            carpeta = "c:\\FEDaemonTEVSUR\\Facturas";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioFacturas(carpeta+"\\");
            
            carpeta = "c:\\FEDaemonTEVSUR\\Retenciones";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioRetenciones(carpeta+"\\");
            
            carpeta = "c:\\FEDaemonTEVSUR\\NC";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioNotasCredito(carpeta+"\\");
            
            carpeta = "c:\\FEDaemonTEVSUR\\ND";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioNotasDebito(carpeta+"\\");
            
            
        }
        if(System.getProperty("os.name").contains("Linux"))
        {
            String carpeta = "FEDaemonTEVSUR";
            File acceso = new File(carpeta);
            
            if (!acceso.exists()) 
                acceso.mkdir();
            servicioTevsur.setDirectorioRaiz(carpeta);
            
            carpeta = "FEDaemonTEVSUR/Facturas";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioFacturas(carpeta+"/");
            
            carpeta = "FEDaemonTEVSUR/Retenciones";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioRetenciones(carpeta+"/");
            
            carpeta = "FEDaemonTEVSUR/NC";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioNotasCredito(carpeta+"/");
            
            carpeta = "FEDaemonTEVSUR/ND";
            acceso = new File(carpeta);
            if (!acceso.exists()) {
                acceso.mkdir();
            }
            servicioTevsur.setDirectorioNotasDebito(carpeta+"/");

        }
    }
    
    
    public static String pid() {
       String id = ManagementFactory.getRuntimeMXBean().getName();
       String[] ids = id.split("@");
       return ids[0];
    }
    
}
