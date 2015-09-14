
package fedaemonfinal.hilos;

import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.dao.FacturaDAO;
import fedaemonfinal.frms.frmMonitor;
import java.sql.SQLException;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarFacturas extends Thread{
    
    protected ConexionBD CONEXION;
    protected frmMonitor MONITOR;
    public FacturaDAO fa;
//    public ThreadAutorizarFacturas(ConexionBD con){
//    this.CONEXION=con;
//    }
    
    @Override
    public void run(){
        
//   ConexionBD CONEXION=new ConexionBD("operaciones","operaciones","192.168.1.10","GLTEVCOL");
//   ConexionBD CONEXION=new ConexionBD("inter","INTER2014","192.168.1.245","GLTEVCOL");
//   ConexionBD CONEXION=new ConexionBD("sistemas","tevsur","192.168.15.200","GLTEVSUR");
        ConexionBD con=null;
   fa=new FacturaDAO(); 
   fa.setMONITOR(MONITOR);
   try{
       con=new ConexionBD(CONEXION.getUsr(),CONEXION.getPass(),CONEXION.getServer(),CONEXION.getBase());
        System.out.println("[info] - Iniciando hilo para autorización de Facturas... ");
        this.MONITOR.setMensajeFacturas("[info] - Iniciando hilo para autorización de Facturas... ");
                     
        while(true)
        {
            this.MONITOR.cambiaEstadoPanel("jPFacturas", "Facturas [EJECUTANDO]");
            this.MONITOR.limpiaFacturas();
            System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
            this.MONITOR.setMensajeFacturas("[info] - Estableciendo conexión con la Base de Datos... ");
            con.conectar();
            
            System.out.println("[info] - Conexión para hilo facturas exitosa");
            this.MONITOR.setMensajeFacturas("[info] - Conexión para hilo facturas exitosa");
//            System.out.println("=================================================");
//            this.MONITOR.setMensajeFacturas("=================================================");
            System.out.println("[info] - Verificando Facturas pendientes de autorización...");
            this.MONITOR.setMensajeFacturas("[info] - Verificando Facturas pendientes de autorización...");
         
//        int contar=fa.consultarFacturasPendientes(con);
        int contar=fa.consultarFacturasPendientes(con,"01");
        fa.cambiaEstado(con, "EJECUTANDO",contar);
        int enviadas=0;
        if(contar==0)
        { 
            System.out.println("[info] - No hay faturas pendientes de autorización.");
            this.MONITOR.setMensajeFacturas("[info] - No hay faturas pendientes de autorización.");
        }
        else
        {
            System.out.println("[info] - Petición de autorización para: "+contar+" facturas");
            this.MONITOR.setMensajeFacturas("[info] - Petición de autorización para: "+contar+" facturas");
            enviadas=fa.enviarFacturas(con);
            System.out.println("[info] - Se han enviado: "+enviadas+" facturas para autorización del SRI.");
            this.MONITOR.setMensajeFacturas("[info] - Se han enviado: "+enviadas+" facturas para autorización del SRI.");

        }
        
            
        System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
        this.MONITOR.setMensajeFacturas("[info] - Cerrando conexión con la Base de Datos... ");
        fa.cambiaEstado(con,"EN ESPERA", 0);
        con.desconectar();
        System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
        this.MONITOR.setMensajeFacturas("[info] - Se ha cerrado la conexión con la base de datos");
        
        System.out.println("[info] - Pausando el Hilo Facturas por 5 minuto(s)");
        this.MONITOR.setMensajeFacturas("[info] - Pausando el Hilo Facturas por 5 minuto(s)");
        this.MONITOR.cambiaEstadoPanel("jPFacturas", "Facturas [EN ESPERA]");
        this.sleep(300000);
        
        
        }
    }
    catch(SQLException sqlex){
        
        System.out.println("[error] - Error de conexión a la base de datos\n"+sqlex.getMessage());
        this.MONITOR.setMensajeFacturas("[Error] - Error de conexión a la base de datos\n"+sqlex.getMessage());
    }
    catch(Exception ex){

        System.out.println("[error] - Se ha detectado un error!\n"+ex.getMessage());
        this.MONITOR.setMensajeFacturas("[error] - Se ha detectado un error!\n"+ex.getMessage());
    }
    finally{
        try {
                con.conectar();
            System.out.println("[info] - Cerrando hilo para autorización de facturas... ");
            this.MONITOR.setMensajeFacturas("[info] - Cerrando hilo para autorización de facturas... ");
            fa.cambiaEstado(con,"APAGADO", 0);
//            this.stop();
            con.desconectar();
            System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
            this.MONITOR.setMensajeFacturas("[info] - Se ha cerrado la conexión con la base de datos");
                
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    }

    public ConexionBD getCONEXION() {
        return CONEXION;
    }

    public void setCONEXION(ConexionBD CONEXION) {
        this.CONEXION = CONEXION;
    }

    public frmMonitor getMONITOR() {
        return MONITOR;
    }

    public void setMONITOR(frmMonitor MONITOR) {
        this.MONITOR = MONITOR;
    }
    
    

}
