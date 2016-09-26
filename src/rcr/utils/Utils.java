package rcr.utils;

/**
 * Clase con métodos estáticos de utilidad
 *
 * @author Roberto Carrasco
 */
public class Utils {
    /**
     * Esta clase no puede ser instanciada
     */
    private Utils() {
    }

    /**
     * Suspende la ejecución del thread un número dado de ms
     *
     * @param ms el tiempo en ms a realizar de pausa
     */
    public static void pause( int ms ) {
        try {
            Thread.sleep( ms );
        } catch( InterruptedException e ) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Para depurar a través de un arreglo de bytes (multithread?)
     *
     * @param b el arreglo de bytes a deslegar
     */
    public static String bytesToHex( byte[] b ) {
        return javax.xml.bind.DatatypeConverter.printHexBinary( b );
    }

    /**
     * Para depurar a través de mensajes de textos (multithread?)
     *
     * @param msg el mensaje a desplegar
     */
    public static void debug( String msg ) {
        System.out.println( msg );
    }

    /**
     * Para depurar a través de mensajes numéricos (multithread?)
     *
     * @param n el número a desplegar
     */
    public static void debug( int n ) {
        System.out.println( n );
    }

    /**
     * Para depurar a través de un arreglo de bytes (multithread?)
     *
     * @param b el arreglo de bytes a deslegar
     */
    public static void debug( byte[] b ) {
        System.out.println( javax.xml.bind.DatatypeConverter.printHexBinary( b ) );
    }

}
