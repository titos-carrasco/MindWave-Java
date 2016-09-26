package rcr.mindwave;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import rcr.utils.Serial;
import rcr.utils.SerialTimeoutException;
import rcr.utils.Utils;

public class MindWave implements Runnable {
    class DataAndError {
        byte[] data;
        String err;

        DataAndError( byte[] data, String err ) {
            this.data = data;
            this.err = err;
        }
    }

    /** la puerta serial a la que se conecta */
    private String port;
    /** el timeout de lectura por byte en ms */
    private int timeout;
    /** Global Headset Unique Identifier - 1er byte */
    private byte ghid_high;
    /** Global Headset Unique Identifier - 2do byte */
    private byte ghid_low;
    /** indicador de conexión activa */
    private boolean connected;
    /** el objeto de la conexión serial */
    private Serial conn;
    /** el objeto con la data recibida desde el mindwave */
    private HMindWaveData hmwd;
    /** controla tiempo de vida del thread */
    private boolean trunning;

    public MindWave( String port, int timeout, int ghid) {
        this.port = port;
        this.timeout = timeout;
        this.ghid_high = (byte)( (ghid & 0xFF00 )>>8 );
        this.ghid_low = (byte)(ghid & 0x00FF );
        connected = false;
        conn = null;
        hmwd = new HMindWaveData( (short)0, (short)0, (short)0, (short)0, 0, 0, 0, 0, 0, 0, 0, 0, 0 );
    }

    public boolean connect() {
        if( connected ) {
            System.out.println( "MindWave Connect(): Ya se encuentra conectado a " + port );
            return true;
        }

        System.out.print( "MindWave Connect(): Intentando conectar a " + port + " => " );
        System.out.flush();
        try {
            conn = new Serial( port, 115200, timeout );
        } catch( NoSuchPortException e ) {
            conn = null;
            System.out.println( e );
            return false;
        } catch( PortInUseException e ) {
            conn = null;
            System.out.println( e );
            return false;
        } catch( UnsupportedCommOperationException e ) {
            conn = null;
            System.out.println( e );
            return false;
        } catch( IOException e ) {
            conn = null;
            System.out.println( e );
            return false;
        }
        System.out.println( "OK" );

        // resetea conexión anterior
        System.out.print( "MindWave Connect(): Limpiando conexión previa => " );
        System.out.flush();
        try {
            // request "Disconnect"
            byte[] packet = new byte[1];
            packet[0] = (byte)0xC1;
            conn.write( packet );
        } catch( IOException e ) {
            conn.close();
            conn = null;
            System.out.println( e );
            return false;
        }
        conn.flushRead( 1000 );
        System.out.println( "OK" );

        // conecta con/sin Global Headset Unique Identifier (ghid)
        try {
            if( ghid_high != 0x00 || ghid_low != 0x00 ) {
                System.out.print( "MindWave Connect(): Enlazando headset => " );
                System.out.flush();
                // request "Connect"
                byte[] packet = new byte[3];
                packet[0] = (byte)0xC0;
                packet[1] = ghid_high;
                packet[2] = ghid_low;
                conn.write( packet );
            } else {
                System.out.print( "MindWave Connect(): Buscando headset => " );
                System.out.flush();
                // request "Auto-Connect"
                byte[] packet = new byte[1];
                packet[0] = (byte)0xC2;
                conn.write( packet );
            }
        } catch( IOException e ) {
            conn.close();
            conn = null;
            System.out.println( e );
            return false;
        }

        // esperamos la respuesta del dongle
        String err = null;
        while( true ) {
            System.out.print( "." );
            System.out.flush();

            // lee respuesta
            DataAndError answer = parsePacket();
            if( answer.err != null ) {
                if( err.equals( "ErrChecksum" ) )     // se deben ignorar los errores de checksum
                    continue;
                err = answer.err;
                break;
            }

            // analiza respuesta
            byte[] payload = answer.data;
            int cmd = payload[0] & 0xFF;
            if( cmd == 0xD0 ) {                     // headset found and connected
                ghid_high = (byte)( payload[2] & 0xFF );
                ghid_low = (byte)( payload[3] & 0xFF );
                break;
            }
            if( cmd == 0xD1 ) {                     // headset not found
                if( ( payload[1] & 0xFF ) == 0x00 )
                    err = "ErrNoHeadsetFound";
                else
                    err = "ErrHeadsetNotFound";
                break;
            }
            if( cmd == 0xD2 ) {                     // headset disconnected
                err = "ErrDisconnected";
                break;
            }
            if( cmd == 0xD3 ) {                     // request denied
                err = "ErrRequestDenied";
                break;
            }
            if( cmd == 0xD4 ) {
                if( ( payload[2] & 0xFF ) == 0x00 ) // dongle in stand by mode
                    break;
                else                                // searching
                    Utils.pause( 1 );
            }
            else
                break;
        }

        if( err != null ) {
            conn.close();
            conn = null;
            System.out.println( " " + err );
            return false;
        }
        System.out.println( "OK" );
        connected = true;

        System.out.println( "MindWave Connect(): Levantando tarea de lectura de datos" );
        trunning = false;
        new Thread( this ).start();
        while( !trunning )
             Utils.pause( 10 );
        return true;
    }

    public void run() {
        trunning = true;
        while( trunning ) {
            // System.out.println( "TRead" );
            // System.out.flush();

            // lee y procesa paquete recibido
            String err = parsePayload();
            if( err != null )
                System.out.println( "MindWave: " + err );

            // requerido para el scheduler
            Utils.pause( 10 );
        }
    }

    public void disconnect( ) {
        if( connected ) {
            System.out.print( "MindWave Disconnect(): Deteniendo Tarea => " );
            System.out.flush();
            trunning = false;
            //self._tread.join()
            System.out.println( "OK" );

            // request "Disconnect"
            System.out.print( "MindWave Disconnect(): Desconectando headset y cerrando puerta => " );
            System.out.flush();
            try {
                byte[] packet = new byte[1];
                packet[0] = (byte)0xC1;
                conn.write( packet );
            } catch( IOException e ) {
            }
            conn.flushRead( 1000 );
            conn.close();
            connected = false;
            conn = null;
            System.out.println( "OK" );
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getGlobalHeadsetID() {
        return String.format( "%02X%02X", ghid_high, ghid_low );
    }

    public HMindWaveData getMindWaveData() {
        synchronized( this ) {
            return new HMindWaveData( hmwd.poorSignalQuality, hmwd.attentionESense, hmwd.meditationESense, hmwd.blinkStrength,
                                      hmwd.rawWave16Bit, hmwd.delta, hmwd.theta, hmwd.lowAlpha, hmwd.highAlpha, hmwd.lowBeta, hmwd.highBeta,
                                      hmwd.lowGamma, hmwd.midGamma );
        }
    }

    protected DataAndError parsePacket() {
        boolean inHeader = true;
        int plength = 0;
        int b;

        while( inHeader ) {
            try {
                b = ( conn.read( 1 )[0] ) & 0xFF;
                if( b == 0xAA ) {
                    b = ( conn.read( 1 )[0] ) & 0xFF;
                    if( b == 0xAA ) {
                        while( true ) {
                            plength = ( conn.read( 1 )[0] ) & 0xFF;
                            if( plength > 0xAA )
                                break;
                            if( plength < 0xAA )
                                inHeader = false;
                                break;
                        }
                    }
                }
            } catch( IOException e ) {
                return new DataAndError( null, "ErrRead" );
            } catch( SerialTimeoutException e ) {
                return new DataAndError( null, "ErrRead" );
            }
        }

        if( plength <= 0 )
            return new DataAndError( null, "ErrZeroPlength" );

        byte[] payload;
        int checksum;
        try {
            payload = conn.read( plength );
            checksum = ( conn.read( 1 )[0] ) & 0xFF;
        } catch ( IOException e ) {
            return new DataAndError( null, "ErrRead" );
        } catch ( SerialTimeoutException e ) {
            return new DataAndError( null, "ErrRead" );
        }
        int suma = 0;
        for( int i=0; i<plength; i++ )
            suma = suma + ( payload[i] & 0xFF );
        suma = ( ~( suma & 0xFF ) ) & 0xFF;
        if( checksum != suma )
            return new DataAndError( null, "ErrChecksum" );
        else
            return new DataAndError( payload, null );
    }

    protected String parsePayload() {
        synchronized( this ) {
            DataAndError dae = parsePacket();
            if( dae.err != null )
                return dae.err;

            byte[] payload = dae.data;
            if( ( payload[0] & 0xFF ) == 0xD2 )        // disconnected
                return "ErrDisconnected";

            if( ( payload[0] & 0xFF ) == 0xD4 )        // alive message in stand by mode
                return null;


            int pos = 0;
            while( pos < payload.length ) {
                int exCodeLevel = 0;
                while( ( payload[pos] & 0xFF ) == 0x55 ) {
                    exCodeLevel = exCodeLevel + 1;
                    pos = pos + 1;
                }
                int code = payload[pos] & 0xFF;
                int vlength;
                pos = pos + 1;
                if( code >= 0x80 ) {
                    vlength = payload[pos] & 0xFF;
                    pos = pos + 1;
                }
                else
                    vlength = 1;

                byte[] data = new byte[ vlength ];
                for( int i=0; i<vlength; i++ )
                    data[i] = payload[pos + i];
                pos = pos + vlength;

                if( exCodeLevel == 0 ) {
                    if( code == 0x02 )        // poor signal quality (0 to 255) 0=>OK; 200 => no skin contact
                        hmwd.poorSignalQuality = (short)( data[0] & 0xFF) ;
                    else if( code == 0x04 )    // attention eSense (0 to 100) 40-60 => neutral, 0 => result is unreliable
                        hmwd.attentionESense = (short)( data[0] & 0xFF );
                    else if( code == 0x05 )    // meditation eSense (0 to 100) 40-60 => neutral, 0 => result is unreliable
                        hmwd.meditationESense = (short)( data[0] & 0xFF );
                    else if( code == 0x16 )    // blink strength (1 to 255)
                        hmwd.blinkStrength = (short)( data[0] & 0xFF );
                    else if( code == 0x80 ) {  // raw wave value (-32768 to 32767) - big endian
                        int n = ( ( data[0] & 0xFF )<<8 ) + ( data[1] & 0xFF );
                        if( n >= 32768 )
                            n = n - 65536;
                        hmwd.rawWave16Bit = n;
                    }
                    else if( code == 0x83 ) {  // asic eeg power struct (8, 3 bytes unsigned int big indian)
                        hmwd.delta     = ( ( data[0] & 0xFF )<<16 ) + ( ( data[1] & 0xFF )<<8 ) + ( data[2] & 0xFF );
                        hmwd.theta     = ( ( data[3] & 0xFF )<<16 ) + ( ( data[4] & 0xFF )<<8 ) + ( data[5] & 0xFF );
                        hmwd.lowAlpha  = ( ( data[6] & 0xFF )<<16 ) + ( ( data[7] & 0xFF )<<8 ) + ( data[8] & 0xFF );
                        hmwd.highAlpha = ( ( data[9] & 0xFF )<<16 ) + ( ( data[10] & 0xFF )<<8 ) + ( data[11] & 0xFF );
                        hmwd.lowBeta   = ( ( data[12] & 0xFF )<<16 ) + ( ( data[13] & 0xFF )<<8 ) + ( data[14] & 0xFF );
                        hmwd.highBeta  = ( ( data[15] & 0xFF )<<16 ) + ( ( data[16] & 0xFF )<<8 ) + ( data[17] & 0xFF );
                        hmwd.lowGamma  = ( ( data[18] & 0xFF )<<16 ) + ( ( data[19] & 0xFF )<<8 ) + ( data[20] & 0xFF );
                        hmwd.midGamma  = ( ( data[21] & 0xFF )<<16 ) + ( ( data[22] & 0xFF )<<8 ) + ( data[23] & 0xFF );
                    }
                    // elif( code == 0x01 )  // code battery - battery low (0x00)
                    // elif( code == 0x03 )  // heart rate (0 to 255)
                    // elif( code == 0x06 )  // 8bit raw wave value (0 to 255)
                    // elif( code == 0x07 )  // raw marker section start (0)
                    // elif( code == 0x81 )  // eeg power struct (legacy float)
                    // elif( code == 0x86 )  // rrinterval (0 to 65535)
                    else
                        System.out.printf( "ExCodeLevel: %02x, Code: %02x, Data: [%s]\n", exCodeLevel, code, Utils.bytesToHex( data ) );
                }
            }
            return null;
        }
    }
}

