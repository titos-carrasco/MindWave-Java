import rcr.mindwave.MindWave;
import rcr.mindwave.HMindWaveData;
import rcr.utils.Utils;

class TestConsola {
    public static void main( String [] args ) throws Exception {
        MindWave mw = new MindWave( "/dev/ttyUSB0", 1000, 0x0000 );
        if( mw.connect() ) {
            for( int i=0; i<1000; i++ ) {
                HMindWaveData mwd = mw.getMindWaveData();
                System.out.print( "Main [" + i + "]: " + mw.getGlobalHeadsetID() + " : " );
                System.out.println( mwd );

                // requerido para el scheduler
                Utils.pause( 10 );
            }
            mw.disconnect();
        }
    }
}
