
package fedaemon.hilos;

import fedaemon.util.ConexionBD;
import fedaemon.dao.NotaCreditoDAO;
import fedaemon.frms.frmMonitor;
import java.sql.SQLException;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarNotasCredito extends Thread{
    
    protected ConexionBD conexionBD;
    protected frmMonitor frmMonitor;
    public  NotaCreditoDAO notaCreditoDAO;
    
    @Override
    public void run(){

        ConexionBD con=null; 
        int enviadas=0;
        int contar=0;
        
        notaCreditoDAO=new NotaCreditoDAO();
        notaCreditoDAO.setMonitor(frmMonitor);
        con=new ConexionBD(conexionBD.getUsr(),conexionBD.getPass(),conexionBD.getServer(),conexionBD.getBase());
        System.out.println("[info] - Iniciando hilo para autorización de Notas de Crédito... ");
        this.frmMonitor.setMensajeNC("[info] - Iniciando hilo para autorización de Notas de Crédito... ");
        
        while(true)
        {
            this.frmMonitor.cambiaEstadoPanel("jPNC", "Notas de Crédito [EJECUTANDO]");
            this.frmMonitor.limpiaNC();
            try{
                System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
                this.frmMonitor.setMensajeNC("[info] - Estableciendo conexión con la Base de Datos... ");
                con.conectar();

                System.out.println("[info] - Conexión para hilo notas de crédito exitosa");
                this.frmMonitor.setMensajeNC("[info] - Conexión para hilo notas de crédito exitosa");
                System.out.println("[info] - Verificando Notas de Crédito pendientes de autorización...");
                this.frmMonitor.setMensajeNC("[info] - Verificando Notas de Crédito pendientes de autorización...");

                contar=notaCreditoDAO.consultarNotaCreditoPendientes(con,"04",frmMonitor.getServicio().getAmbiente());
                notaCreditoDAO.cambiaEstado(con,"EJECUTANDO", contar);
        
                if(contar==0)
                { 
                    System.out.println("[info] - No hay Notas de Crédito pendientes de autorización.");
                    this.frmMonitor.setMensajeNC("[info] - No hay Notas de Crédito pendientes de autorización.");
                }
                else
                {
                    System.out.println("[info] - Petición de autorización para: "+contar+" notas de crédito");
                    this.frmMonitor.setMensajeNC("[info] - Petición de autorización para: "+contar+" notas de crédito");
                    enviadas=notaCreditoDAO.enviarNotasCredito(con);
                    System.out.println("[info] - Se han enviado: "+enviadas+" notas de crédito para autorización del SRI.");
                    this.frmMonitor.setMensajeNC("[info] - Se han enviado: "+enviadas+" notas de crédito para autorización del SRI.");

                }
            }
            catch(SQLException sqlex)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
                this.frmMonitor.setMensajeNC("[Error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
            }
            catch(ClassNotFoundException cnfe)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
                this.frmMonitor.setMensajeNC("[Error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
            }
            finally
            {
                try
                {
                    System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
                    this.frmMonitor.setMensajeNC("[info] - Cerrando conexión con la Base de Datos... ");
                    notaCreditoDAO.cambiaEstado(con,"EN ESPERA", 0);
                    con.desconectar();
                    System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
                    this.frmMonitor.setMensajeNC("[info] - Se ha cerrado la conexión con la base de datos");
                }
                catch(SQLException sqle)
                {
                    System.out.println("[error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                    this.frmMonitor.setMensajeNC("[Error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                }
                finally
                {
                    System.out.println("[info] - Continuando...");
                    this.frmMonitor.setMensajeNC("[info] - Continuando...");
                }
            }
        
            try{
                System.out.println("[info] - Pausando el Hilo Notas de Crédito por 5 minuto(s)");
                this.frmMonitor.setMensajeNC("[info] - Pausando el Hilo Notas de Crédito por 5 minuto(s)");
                this.frmMonitor.cambiaEstadoPanel("jPNC", "Notas de Crédito [EN ESPERA]");
                this.sleep(300000);
            }
            catch (Exception ex)
            {
                System.out.println("[error] - Error al pausar el hilo");
                this.frmMonitor.setMensajeNC("[error] - Error al pausar el hilo");
            }
            finally
            {
                System.out.println("[info] - Continuando...");
                this.frmMonitor.setMensajeNC("[info] - Continuando...");
            }
        
        }//final del while
    
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
