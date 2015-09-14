

package fedaemonfinal.hilos;

import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.util.FEDaemonFINAL;
import fedaemonfinal.dao.NotaDebitoDAO;
import fedaemonfinal.frms.frmMonitor;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarNotasDebito extends Thread{
    
    protected ConexionBD CONEXION;
    protected frmMonitor MONITOR;
    public NotaDebitoDAO ret;
    
    @Override
    public void run(){
    //    ConexionBD CONEXION=new ConexionBD("operaciones","operaciones","192.168.1.10","GLTEVCOL");
//      ConexionBD CONEXION=new ConexionBD("inter","INTER2014","192.168.1.245","GLTEVCOL");
//      ConexionBD CONEXION=new ConexionBD("sistemas","tevsur","192.168.15.200","GLTEVSUR");
        ConexionBD con=new ConexionBD(CONEXION.getUsr(),CONEXION.getPass(),CONEXION.getServer(),CONEXION.getBase());
      ret=new NotaDebitoDAO();
      ret.setMONITOR(MONITOR);
      try{
        System.out.println("[info] - Iniciando hilo para autorización de Notas de Débito... ");
        this.MONITOR.setMensajeND("[info] - Iniciando hilo para autorización de Notas de Débito... ");
        
                     
        while(true)
        {
            this.MONITOR.cambiaEstadoPanel("jPND", "Notas de Dédito [EJECUTANDO]");
            this.MONITOR.limpiaND();
        System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
        this.MONITOR.setMensajeND("[info] - Estableciendo conexión con la Base de Datos... ");
        con.conectar();
        
        System.out.println("[info] - Conexión para hilo notas de débito exitosa");
        this.MONITOR.setMensajeND("[info] - Conexión para hilo notas de débito exitosa");
//        System.out.println("=================================================");
//        this.MONITOR.setMensajeND("=================================================");
        System.out.println("[info] - Verificando Notas de Débito pendientes de autorización...");
        this.MONITOR.setMensajeND("[info] - Verificando Notas de Débito pendientes de autorización...");
            
//        int contar=ret.consultarNotasDebitoPendientes(con);
        int contar=ret.consultarNotasDebitoPendientes(con,"05");
        ret.cambiaEstado(con, "EJECUTANDO", contar);
        int enviadas=0;
        if(contar==0)
        { 
            System.out.println("[info] - No hay Notas de Débito pendientes de autorización.");
            this.MONITOR.setMensajeND("[info] - No hay Notas de Débito pendientes de autorización.");
        }
        else
        {
            System.out.println("[info] - Petición de autorización para: "+contar+" notas de dédito");
            this.MONITOR.setMensajeND("[info] - Petición de autorización para: "+contar+" notas de dédito");
            enviadas=ret.enviarNotasDebito(con);
            System.out.println("[info] - Se han enviado: "+enviadas+" notas de débito para autorización del SRI.");
            this.MONITOR.setMensajeND("[info] - Se han enviado: "+enviadas+" notas de débito para autorización del SRI.");

        }
        System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
        this.MONITOR.setMensajeND("[info] - Cerrando conexión con la Base de Datos... ");
        ret.cambiaEstado(con, "EN ESPERA", 0);
        con.desconectar();
        System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
            this.MONITOR.setMensajeND("[info] - Se ha cerrado la conexión con la base de datos");
        
        System.out.println("[info] - Pausando el Hilo Notas de Débito por 5 minuto(s)");
        this.MONITOR.setMensajeND("[info] - Pausando el Hilo Notas de Débito por 5 minuto(s)");
        this.MONITOR.cambiaEstadoPanel("jPND", "Notas de Dédito [EN ESPERA]");
        this.sleep(300000);
        
        
        }
    }
    catch(SQLException sqlex){
        System.out.println("[Error] - Error de conexión a la base de datos");
        this.MONITOR.setMensajeND("[Error] - Error de conexión a la base de datos");
        sqlex.printStackTrace();
    }
    catch(Exception ex){
        System.out.println("[Error] - Se ha detectado un error!");
        this.MONITOR.setMensajeND("[Error] - Se ha detectado un error!");
        ex.printStackTrace();
    }
    finally{
        try {
            con.conectar();
            System.out.println("[info] - Cerrando hilo para autorización de notas de débito... ");
            this.MONITOR.setMensajeND("[info] - Cerrando hilo para autorización de notas de débito... ");
            ret.cambiaEstado(con,"APAGADO", 0);
            this.stop();
            con.desconectar();
            System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
            this.MONITOR.setMensajeND("[info] - Se ha cerrado la conexión con la base de datos");
        } catch (Exception ex) {
            Logger.getLogger(FEDaemonFINAL.class.getName()).log(Level.SEVERE, null, ex);
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
