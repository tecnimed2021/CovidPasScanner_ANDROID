package it.tecnimed.covidpasscanner.Tecnimed;

import java.io.Serializable;

public class AppSetup implements Serializable
{
    final public static int ORAL = 0;
    final public static int RECTAL = 1;
    final public static int AXILLA = 2;
    final public static int CORE = 3;
    public static boolean SequenceTemperature = false;
    public static boolean SequenceGreenPass = false;
    public static boolean SequenceDocument = false;

    public static float RangeTempGreen = 37.5f;
    public static float RangeTempOrange = 0.5f;

    public static int TempCorrection = ORAL;
    public static boolean TempCorrectionAir = false;

    public boolean getSequenceTemperature()
    {
        return SequenceTemperature;
    }
    public void setSequenceTemperature(boolean st)
    {
        SequenceTemperature = st;
    }
    public boolean getSequenceGreenPass()
    {
        return SequenceGreenPass;
    }
    public void setSequenceGreenPass(boolean st)
    {
        SequenceGreenPass = st;
    }
    public boolean getSequenceDocument()
    {
        return SequenceDocument;
    }
    public void setSequenceDocument(boolean st)
    {
        SequenceDocument = st;
    }
    public float getRangeGreen()
    {
        return RangeTempGreen;
    }
    public void setRangeGreen(float t)
    {
        RangeTempGreen = t;
    }
    public float getRangeOrange()
    {
        return RangeTempOrange;
    }
    public void setRangeOrange(float t)
    {
        RangeTempOrange = t;
    }
    public int getCorrection()
    {
        return TempCorrection;
    }
    public void setCorrection(int t)
    {
        TempCorrection = t;
    }
    public boolean getCorrectionAir()
    {
        return TempCorrectionAir;
    }
    public void setCorrectionAir(boolean t)
    {
        TempCorrectionAir = t;
    }
}
