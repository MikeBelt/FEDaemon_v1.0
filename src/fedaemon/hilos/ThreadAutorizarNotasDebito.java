

package fedaemon.hilos;

import fedaemon.util.ConexionBD;
import fedaemon.util.FEDaemonFINAL;
import fedaemon.dao.NotaDebitoDAO;
import fedaemon.frms.frmMonitor;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarNotasDebito extends Thread{
    
    protected ConexionBD conexionBD;
    protected frmMonitor frmMonitor;
    public NotaDebitoDAO notaDebitoDAO;
    
    @Override
    public void run(){
    //    ConexionBD conexionBD=new ConexionBD("operaciones","operaciones","192.168.1.10","GLTEVCOL");
//      ConexionBD conexionBD=new ConexionBD("inter","INTER2014","192.168.1.245","GLTEVCOL");
//      ConexionBD conexionBD=new ConexionBD("sistemas","tevsur","192.168.15.200","GLTEVSUR");
        ConexionBD con=new ConexionBD(conexionBD.getUsr(),conexionBD.getPass(),conexionBD.getServer(),conexionBD.getBase());
      notaDebitoDAO=new NotaDebitoDAO();
      notaDebitoDAO.setMONITOR(frmMonitor);
      try{
        System.out.println("[info] - Iniciando hilo para autorización de Notas de Débito... ");
        this.frmMonitor.setMensajeND("[info] - Iniciando hilo para autorización de Notas de Débito... ");
        
                     
        while(true)
        {
            this.frmMonitor.cambiaEstadoPanel("jPND", "Notas de Dédito [EJECUTANDO]");
            this.frmMonitor.limpiaND();
        System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
        this.frmMonitor.setMensajeND("[info] - Estableciendo conexión con la Base de Datos... ");
        con.conectar();
        
        System.out.println("[info] - Conexión para hilo notas de débito exitosa");
        this.frmMonitor.setMensajeND("[info] - Conexión para hilo notas de débito exitosa");
//        System.out.println("=================================================");
//        this.frmMonitor.setMensajeND("=================================================");
        System.out.println("[info] - Verificando Notas de Débito pendientes de autorización...");
        this.frmMonitor.setMensajeND("[info] - Verificando Notas de Débito pendientes de autorización...");
            
//        int contar=notaDebitoDAO.consultarNotasDebitoPendientes(con);
        int contar=notaDebitoDAO.consultarNotasDebitoPendientes(con,"05");
        notaDebitoDAO.cambiaEstado(con, "EJECUTANDO", contar);
        int enviadas=0;
        if(contar==0)
        { 
            System.out.println("[info] - No hay Notas de Débito pendientes de autorización.");
            this.frmMonitor.setMensajeND("[info] - No hay Notas de Débito pendientes de autorización.");
        }
        else
        {
            System.out.println("[info] - Petición de autorización para: "+contar+" notas de dédito");
            this.frmMonitor.setMensajeND("[info] - Petición de autorización para: "+contar+" notas de dédito");
            enviadas=notaDebitoDAO.enviarNotasDebito(con);
            System.out.println("[info] - Se han enviado: "+enviadas+" notas de débito para autorización del SRI.");
            this.frmMonitor.setMensajeND("[info] - Se han enviado: "+enviadas+" notas de débito para autorización del SRI.");

        }
        System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
        this.frmMonitor.setMensajeND("[info] - Cerrando conexión con la Base de Datos... ");
        notaDebitoDAO.cambiaEstado(con, "EN ESPERA", 0);
        con.desconectar();
        System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
            this.frmMonitor.setMensajeND("[info] - Se ha cerrado la conexión con la base de datos");
        
        System.out.println("[info] - Pausando el Hilo Notas de Débito por 5 minuto(s)");
        this.frmMonitor.setMensajeND("[info] - Pausando el Hilo Notas de Débito por 5 minuto(s)");
        this.frmMonitor.cambiaEstadoPanel("jPND", "Notas de Dédito [EN ESPERA]");
        this.sleep(300000);
        
        
        }
    }
    catch(SQLException sqlex){
        System.out.println("[Error] - Error de conexión a la base de datos");
        this.frmMonitor.setMensajeND("[Error] - Error de conexión a la base de datos");
        sqlex.printStackTrace();
    }
    catch(Exception ex){
        System.out.println("[Error] - Se ha detectado un error!");
        this.frmMonitor.setMensajeND("[Error] - Se ha detectado un error!");
        ex.printStackTrace();
    }
    finally{
        try {
            con.conectar();
            System.out.println("[info] - Cerrando hilo para autorización de notas de débito... ");
            this.frmMonitor.setMensajeND("[info] - Cerrando hilo para autorización de notas de débito... ");
            notaDebitoDAO.cambiaEstado(con,"APAGADO", 0);
            this.stop();
            con.desconectar();
            System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
            this.frmMonitor.setMensajeND("[info] - Se ha cerrado la conexión con la base de datos");
        } catch (Exception ex) {
            Logger.getLogger(FEDaemonFINAL.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    }
    
    public ConexionBD getConexion() {
        return conexionBD;
    }

    public void setConexion(ConexionBD conexion) {
        this.conexionBD = conexion;
    }
    
    public frmMonitor getMonitor() {
        return frmMonitor;
    }

    public void setMonitor(frmMonitor monitor) {
        this.frmMonitor = monitor;
    }
    
}
