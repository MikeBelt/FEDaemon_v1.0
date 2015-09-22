
package fedaemonfinal.hilos;

import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.util.FEDaemonFINAL;
import fedaemonfinal.dao.NotaCreditoDAO;
import fedaemonfinal.frms.frmMonitor;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarNotasCredito extends Thread{
    
    protected ConexionBD CONEXION;
    protected frmMonitor MONITOR;
    public  NotaCreditoDAO notaCreditoDAO;
    
    @Override
    public void run(){
//    ConexionBD CONEXION=new ConexionBD("operaciones","operaciones","192.168.1.10","GLTEVCOL");
//    ConexionBD CONEXION=new ConexionBD("inter","inter2014","192.168.1.245","GLTEVCOL");
//    ConexionBD CONEXION=new ConexionBD("sistemas","tevsur","192.168.15.200","GLTEVSUR");
        ConexionBD con=null; 
        int enviadas=0;
        int contar=0;
        
        notaCreditoDAO=new NotaCreditoDAO();
        notaCreditoDAO.setMonitor(MONITOR);
        con=new ConexionBD(CONEXION.getUsr(),CONEXION.getPass(),CONEXION.getServer(),CONEXION.getBase());
        System.out.println("[info] - Iniciando hilo para autorización de Notas de Crédito... ");
        this.MONITOR.setMensajeNC("[info] - Iniciando hilo para autorización de Notas de Crédito... ");
        
        while(true)
        {
            this.MONITOR.cambiaEstadoPanel("jPNC", "Notas de Crédito [EJECUTANDO]");
            this.MONITOR.limpiaNC();
            try{
                System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
                this.MONITOR.setMensajeNC("[info] - Estableciendo conexión con la Base de Datos... ");
                con.conectar();

                System.out.println("[info] - Conexión para hilo notas de crédito exitosa");
                this.MONITOR.setMensajeNC("[info] - Conexión para hilo notas de crédito exitosa");
                System.out.println("[info] - Verificando Notas de Crédito pendientes de autorización...");
                this.MONITOR.setMensajeNC("[info] - Verificando Notas de Crédito pendientes de autorización...");

                contar=notaCreditoDAO.consultarNotaCreditoPendientes(con,"04");
                notaCreditoDAO.cambiaEstado(con,"EJECUTANDO", contar);
        
                if(contar==0)
                { 
                    System.out.println("[info] - No hay Notas de Crédito pendientes de autorización.");
                    this.MONITOR.setMensajeNC("[info] - No hay Notas de Crédito pendientes de autorización.");
                }
                else
                {
                    System.out.println("[info] - Petición de autorización para: "+contar+" notas de crédito");
                    this.MONITOR.setMensajeNC("[info] - Petición de autorización para: "+contar+" notas de crédito");
                    enviadas=notaCreditoDAO.enviarNotasCredito(con);
                    System.out.println("[info] - Se han enviado: "+enviadas+" notas de crédito para autorización del SRI.");
                    this.MONITOR.setMensajeNC("[info] - Se han enviado: "+enviadas+" notas de crédito para autorización del SRI.");

                }
            }
            catch(SQLException sqlex)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
                this.MONITOR.setMensajeNC("[Error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
            }
            catch(ClassNotFoundException cnfe)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
                this.MONITOR.setMensajeNC("[Error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
            }
            finally
            {
                try
                {
                    System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
                    this.MONITOR.setMensajeNC("[info] - Cerrando conexión con la Base de Datos... ");
                    notaCreditoDAO.cambiaEstado(con,"EN ESPERA", 0);
                    con.desconectar();
                    System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
                    this.MONITOR.setMensajeNC("[info] - Se ha cerrado la conexión con la base de datos");
                }
                catch(SQLException sqle)
                {
                    System.out.println("[error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                    this.MONITOR.setMensajeNC("[Error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                }
                finally
                {
                    System.out.println("[info] - Continuando...");
                    this.MONITOR.setMensajeNC("[info] - Continuando...");
                }
            }
        
            try{
                System.out.println("[info] - Pausando el Hilo Notas de Crédito por 5 minuto(s)");
                this.MONITOR.setMensajeNC("[info] - Pausando el Hilo Notas de Crédito por 5 minuto(s)");
                this.MONITOR.cambiaEstadoPanel("jPNC", "Notas de Crédito [EN ESPERA]");
                this.sleep(300000);
            }
            catch (Exception ex)
            {
                System.out.println("[error] - Error al pausar el hilo");
                this.MONITOR.setMensajeNC("[error] - Error al pausar el hilo");
            }
            finally
            {
                System.out.println("[info] - Continuando...");
                this.MONITOR.setMensajeNC("[info] - Continuando...");
            }
        
        }//final del while
    
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
