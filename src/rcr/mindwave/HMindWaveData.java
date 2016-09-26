package rcr.mindwave;

/**
 * Clase de apoyo para almacenar la data enviada por el MindWave
 * <p>
 * La data puede ser accesada directamente, por ejemplo como <code>obj.attentionESense</code>
 *
 * @see MindWave
 * @author Roberto Carrasco
 */
public class HMindWaveData {
    /** poor signal quality - byte - (0 <=> 200) 0=OK; 200=sensor sin contacto con la piel */
    public short poorSignalQuality;
    /** attention eSense - byte - (1 <=> 100) 0=no confiable */
    public short attentionESense;
    /** meditation eSense - byte - (1 <=> 100) 0=no confiable */
    public  short meditationESense;
    /** blink strength - byte - (1 <=> 255) */
    public  short blinkStrength;
    /** raw wave 16bit format -int16 (-32768 <=> 32767) */
    public  int rawWave16Bit;
    /**  delta - uint24 - (0 <=> 16777215) */
    public  int delta;
    /**  theta - uint24 - (0 <=> 16777215) */
    public  int theta;
    /**  low alpha - uint24 - (0 <=> 16777215) */
    public  int lowAlpha;
    /**  high alpha - uint24 - (0 <=> 16777215) */
    public  int highAlpha;
    /**  low beta - uint24 - (0 <=> 16777215) */
    public  int lowBeta;
    /**  high beta - uint24 - (0 <=> 16777215) */
    public  int highBeta;
    /**  low gamma - uint24 - (0 <=> 16777215) */
    public  int lowGamma;
    /**  mid gamma - uint24 - (0 <=> 16777215) */
    public  int midGamma;

    /**
     * Construye un objeto con la data del MindWave
     *
     * @param poorSignalQuality poor signal quality
     * @param attentionESense attention eSense
     * @param meditationESense meditation eSense
     * @param rawWave16Bit raw wave
     * @param delta delta
     * @param theta theta
     * @param lowAlpha low alpha
     * @param highAlpha high alpha
     * @param lowBeta low beta
     * @param highBeta high beta
     * @param lowGamma low gamma
     * @param midGamma mid gamma
     */
    public HMindWaveData( short poorSignalQuality, short attentionESense, short meditationESense, short blinkStrength, int rawWave16Bit,
                          int delta, int theta, int lowAlpha, int highAlpha, int lowBeta, int highBeta, int lowGamma, int midGamma ) {
        this.poorSignalQuality = poorSignalQuality;
        this.attentionESense = attentionESense;
        this.meditationESense = meditationESense;
        this.blinkStrength = blinkStrength;
        this.rawWave16Bit = rawWave16Bit;
        this.delta = delta;
        this.theta = theta;
        this.lowAlpha = lowAlpha;
        this.highAlpha = highAlpha;
        this.lowBeta = lowBeta;
        this.highBeta = highBeta;
        this.lowGamma = lowGamma;
        this.midGamma = midGamma;
    }

    /**
     * Retorna un objeto del tipo String representando el valor del conjunto de datos
     *
     *  @return un String representando el conjunto de datos
     */
    public String toString() {
        return "HMindWaveData(" + poorSignalQuality + ", " + attentionESense + ", " + meditationESense + ", " + blinkStrength + ", " + rawWave16Bit + ", " +
                                  delta + ", " + theta + ", " + lowAlpha + ", " + highAlpha + ", " +
                                  lowBeta + ", " + highBeta + ", " + lowGamma + ", " + midGamma + ")";
    }
}
