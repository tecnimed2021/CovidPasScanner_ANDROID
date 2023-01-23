package it.tecnimed.covidpasscanner.Tecnimed;

import java.io.Serializable;

public class AppSetup implements Serializable
{
    public static boolean SequenceTemperature = false;
    public static boolean SequenceGreenPass = false;
    public static boolean SequenceDocument = false;

    public static float RangeTempGreen = 37.5f;
    public static float RangeTempOrange = 0.5f;

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
}
