package it.wolfsafety.wolflunar4g.VL;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import it.tecnimed.covidpasscanner.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by lucavaltellina on 28/08/14.
 */
public class VLUtils
{
    public static String loadStringFromRawResource(Resources resources, int resId)
    {
        InputStream rawResource = resources.openRawResource(resId);
        String content = streamToString(rawResource);
        try
        {
            rawResource.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return content;
    }

    private static String streamToString(InputStream in)
    {
        String l;
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        StringBuilder s = new StringBuilder();
        try
        {
            while ((l = r.readLine()) != null) {
                s.append((l + "\n"));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return s.toString();
    }


    public static String removeStringTerminator(String s)
    {
        int terminator = s.indexOf(0);

        if(terminator != -1)
            return s.substring(0, terminator);

        return s;
    }

    public static Bitmap compressBitmap(Bitmap src, Bitmap.CompressFormat format, int quality)
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        src.compress(format, quality, os);

        byte[] array = os.toByteArray();
        return BitmapFactory.decodeByteArray(array, 0, array.length);
    }

    public static String sqlStandardFormatStringFromDate()
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }




    // Lettura da InputStream
    public static String GetInputStreamStringData(InputStream instream)
    {
        byte[] buffer = new byte[1024];
        int nread = 0;
        String sstream = null;
        try {
            ByteArrayOutputStream outstreambyte = new ByteArrayOutputStream();
            while ((nread = instream.read(buffer)) != -1) {
                outstreambyte.write(buffer, 0, nread);
            }
            instream.close();
            sstream = new String(outstreambyte.toByteArray());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return sstream;
    }

    public static byte[] GetInputStreamRawData(InputStream instream)
    {
        byte[] buffer = new byte[1024];
        int nread = 0;
        ByteArrayOutputStream outstreambyte = new ByteArrayOutputStream();
        try {
            while ((nread = instream.read(buffer)) != -1) {
                outstreambyte.write(buffer, 0, nread);
            }
            instream.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return outstreambyte.toByteArray();
    }





    // Gestione Lettura e Scrittura su File
    public static Bitmap ReadBitmapFromInternalFile(Application app, String fname)
    {
        String fn = new File(app.getFilesDir(), fname).getAbsolutePath();
        return BitmapFactory.decodeFile(fn);
    }

    public static byte[] ReadBitmapRawDataFromInternalFile(Application app, String fname) {
        String fn = new File(app.getFilesDir(), fname).getAbsolutePath();
        Bitmap bmp = BitmapFactory.decodeFile(fn);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    public static void WriteBitmapToInternalFile(Application app, String fname, Bitmap bmp)
    {
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = app.openFileOutput(fname, Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void WriteBitmapRawDataToInternalFile(Application app, String fname, byte[] imgdatabuffer)
    {
        // Scrittura file immagine
        FileOutputStream fileOutputStream;
        try {
            fileOutputStream = app.openFileOutput(fname, Context.MODE_PRIVATE);
            fileOutputStream.write(imgdatabuffer);
            fileOutputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void CopyAssetsFileToFile(Application app, String assetfname, String fname)
    {
        try{
            AssetManager am = app.getAssets();
            InputStream in = am.open(assetfname);
            FileOutputStream out = app.openFileOutput(fname, Context.MODE_PRIVATE);
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void CopyFromInternalStorageToExternalStorage(Application app, String fname) {
        File srcFile = new File(app.getFilesDir(), fname);
        String dstPath = Environment.getExternalStorageDirectory() + File.separator + fname;
        File dstFile = new File(dstPath);
        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(srcFile).getChannel();
            outChannel = new FileOutputStream(dstFile).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                if (inChannel != null)
                    inChannel.close();
                if (outChannel != null)
                    outChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gestione array di byte
    public static byte[] bytearrayAllocateAndSet(int size, byte value)
    {
        byte[] bb = new byte[size];
        for (int i = 0; i < size; i++) {
            bb[i] = value;
        }
        return bb;
    }

    public static byte[] bytearrayAllocateAndCopy(int size, byte[] src)
    {
        byte[] bb = new byte[size];
        for (int i = 0; i < size; i++) {
            bb[i] = src[i];
        }
        return bb;
    }

    public static int bytearrayFindStringOccurrence(byte[] b, int blen, int offset, String s, boolean end)
    {
        byte[] bs = s.getBytes();
        int tagid = -1;
        boolean tagfound = false;

        for (int i = offset; i < blen; i++)
        {
            tagid = i;
            tagfound = true;
            for (int j = 0; j < bs.length; j++) {
                if(b[i+j] != bs[j]) {
                    tagfound = false;
                    break;
                }
            }
            if(tagfound == true)
            {
                if(end == true)
                    tagid += s.length()-1;
                break;
            }
        }

        return (tagfound ? tagid : -1);
    }

    //
    // From byte array
    //
    public static byte[] bytearrayExtractByteArrayFromTags(byte[] b, int blen, String tags, String tage)
    {
        int idtags = -1;
        int idtage = -1;
        int offset = 0;

        idtags = bytearrayFindStringOccurrence(b, blen, 0, tags, true);
        if(idtags == -1)
            return null;

        idtage = 0;
        while(idtage < idtags) {
            idtage = bytearrayFindStringOccurrence(b, blen, offset, tage, false);
            if(idtage == -1)
                return null;
            offset = idtage+1;
        }
        if (idtags > 0 && idtage > 0) {
            idtags++;
            idtage--;
            byte[] bb =new byte[idtage-idtags+1];
            for (int i = 0; i < bb.length; i++) {
                bb[i] = b[idtags+i];
            }
            return bb;
        }

        return null;
    }

    public static byte bytearrayExtractByteFromTags(byte[] b, int blen, String tags, String tage) throws NumberFormatException
    {
        int idtags = -1;
        int idtage = -1;
        byte value;

        idtags = bytearrayFindStringOccurrence(b, blen, 0, tags, true);
        idtage = bytearrayFindStringOccurrence(b, blen, 0, tage, false);

        if(idtags > 0 && idtage > 0)
        {
            idtags++;
            idtage--;
            if(idtage - idtags + 1 == 1)
            {
                value = (byte)b[idtags];
                return value;
            }
        }
        throw new NumberFormatException("VLUtils. Unrecognized byte");
    }

    public static int bytearrayExtractIntFromTags(byte[] b, int blen, String tags, String tage) throws NumberFormatException
    {
        int idtags = -1;
        int idtage = -1;
        int value;

        idtags = bytearrayFindStringOccurrence(b, blen, 0, tags, true);
        idtage = bytearrayFindStringOccurrence(b, blen, 0, tage, false);

        if(idtags > 0 && idtage > 0)
        {
            idtags++;
            idtage--;
            if(idtage - idtags + 1 == 2)
            {
                value = (byte)b[idtags] & 0x000000FF;
                value += ((((byte)b[idtags+1]) << 8) & 0x0000FF00);
                return value;
            }
        }
        throw new NumberFormatException("VLUtils. Unrecognized int");
    }

    public static long bytearrayExtractLongFromTags(byte[] b, int blen, String tags, String tage) throws NumberFormatException
    {
        int idtags = -1;
        int idtage = -1;
        long value;

        idtags = bytearrayFindStringOccurrence(b, blen, 0, tags, true);
        idtage = bytearrayFindStringOccurrence(b, blen, 0, tage, false);

        if(idtags > 0 && idtage > 0)
        {
            idtags++;
            idtage--;
            if(idtage - idtags + 1 == 4)
            {
                value = (byte)b[idtags] & 0x000000FF;
                value += ((((byte)b[idtags+1]) << 8) & 0x0000FF00);
                value += ((((byte)b[idtags+2]) << 16) & 0x00FF0000);
                value += ((((byte)b[idtags+3]) << 24) & 0xFF000000);
                return value;
            }
        }
        throw new NumberFormatException("VLUtils. Unrecognized int");
    }

    public static String bytearrayToString(byte[] b, int ofs, int len)
    {
        String s = new String(b, ofs, len);

        return s;
    }

    public static byte bytearrayToByte(byte[] b, int ofs)
    {
        byte value = 0;
        value = (byte)b[ofs+0];

        return value;
    }

    public static int bytearrayToInt(byte[] b, int ofs, boolean endian)
    {
        int value = 0;

        if(endian == false) {
            value = (byte) b[ofs + 0] & 0x000000FF;
            value += ((((byte) b[ofs + 1]) << 8) & 0x0000FF00);
        }
        else {
            value = (byte) b[ofs + 1] & 0x000000FF;
            value += ((((byte) b[ofs + 0]) << 8) & 0x0000FF00);
        }

        return value;
    }

    public static long bytearrayToLong(byte[] b, int ofs, boolean endian)
    {
        long value = 0;

        if(endian == false) {
            value = (byte) b[ofs + 0] & 0x000000FF;
            value += ((((byte) b[ofs + 1]) << 8) & 0x0000FF00);
            value += ((((byte) b[ofs + 2]) << 16) & 0x00FF0000);
            value += ((((byte) b[ofs + 3]) << 24) & 0xFF000000);
        }
        else{
            value = (byte) b[ofs + 3] & 0x000000FF;
            value += ((((byte) b[ofs + 2]) << 8) & 0x0000FF00);
            value += ((((byte) b[ofs + 1]) << 16) & 0x00FF0000);
            value += ((((byte) b[ofs + 0]) << 24) & 0xFF000000);
        }

        return value;
    }

    public static float bytearrayToFloat(byte[] b, int ofs, boolean endian)
    {
        int value = 0;

        if(endian == false) {
            value = (byte) b[ofs + 0] & 0x000000FF;
            value += ((((byte) b[ofs + 1]) << 8) & 0x0000FF00);
            value += ((((byte) b[ofs + 2]) << 16) & 0x00FF0000);
            value += ((((byte) b[ofs + 3]) << 24) & 0xFF000000);
        }
        else {
            value = (byte) b[ofs + 3] & 0x000000FF;
            value += ((((byte) b[ofs + 2]) << 8) & 0x0000FF00);
            value += ((((byte) b[ofs + 1]) << 16) & 0x00FF0000);
            value += ((((byte) b[ofs + 0]) << 24) & 0xFF000000);
        }

        return Float.intBitsToFloat(value);
    }

    //
    // To byte array
    //
    public static byte[] byteToBytearray(byte value)
    {
        byte[] bb = new byte[1];
        bb[0] = value;

        return bb;
    }

    public static byte[] intToBytearray(int value)
    {
        byte[] bb = new byte[2];
        bb[0] = (byte)(value & 0x000000FF);
        bb[1] = (byte)((value & 0x0000FF00) >> 8);

        return bb;
    }

    public static byte[] longToBytearray(long value)
    {
        byte[] bb = new byte[4];
        bb[0] = (byte)(value & 0x000000FF);
        bb[1] = (byte)((value & 0x0000FF00) >> 8);
        bb[2] = (byte)((value & 0x00FF0000) >> 16);
        bb[3] = (byte)((value & 0xFF000000) >> 24);

        return bb;
    }

    public static byte[] floatToBytearray(float value)
    {
        byte[] bb = new byte[4];

        int bits = Float.floatToIntBits(value);

        bb[0] = (byte)(bits & 0x000000FF);
        bb[1] = (byte)((bits & 0x0000FF00) >> 8);
        bb[2] = (byte)((bits & 0x00FF0000) >> 16);
        bb[3] = (byte)((bits & 0xFF000000) >> 24);

        return bb;
    }





    // Check user input
    public static String formatTelephoneNumberWithMaxLen(String num, int expectedmaxlen)
    {
        String newnumber = num;
        int len;

        if(num == null)
            return null;

        if(expectedmaxlen == 0)
            return null;

        if(expectedmaxlen > 0)
        {
            if(newnumber.length() == 0)
                return "";

            if(newnumber.length() < 2)
                return null;

            if(newnumber.length() > expectedmaxlen)
                return null;
        }

        if ((newnumber.getBytes()[0] != '0' || newnumber.getBytes()[1] != '0') && newnumber.getBytes()[0] != '+')
            return null;

        if (newnumber.getBytes()[0] == '0' && newnumber.getBytes()[1] == '0')
        {
            newnumber = newnumber.substring(2);
            newnumber = "+" + newnumber;
        }
        for(int i=0; i<newnumber.length(); i++)
        {
            if(newnumber.getBytes()[0] == '+')
                continue;
            if(newnumber.getBytes()[i] < '0' || newnumber.getBytes()[i] > '9')
                return null;
        }

        return newnumber;
    }

    public static boolean isStringNumberWithLen(String num, int expectedlen)
    {
        int len;

        if(num == null)
            return false;

        if(expectedlen > 0)
        {
            if(num.length() > expectedlen)
                return false;
        }
        else if(expectedlen < 0) {
            if(num.length() != -expectedlen)
                return false;
        }
        else
            return false;

        for(int i=0; i<num.length(); i++)
        {
            if(num.getBytes()[i] < '0' || num.getBytes()[i] > '9')
                return false;
        }

        return true;
    }





    // Keyboard
    public static void KbdDismiss(Activity mActivity)
    {
        InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(mActivity != null) {
            if (mActivity.getWindow().getCurrentFocus() != null)
                imm.hideSoftInputFromWindow(mActivity.getWindow().getCurrentFocus().getWindowToken(), 0);
        }
    }





    // Drawable Id
    public static int getDrawableResIdFromName(String drawableName)
    {
        int drawableId = -1;

        try {
            Class res = R.drawable.class;
            Field field = res.getField(drawableName);
            drawableId = field.getInt(null);
        }
        catch (Exception e) {
        }

        return drawableId;
    }




/*
    // Customizzazione ActionBar Title
    public static void setCustomActionBarTitleForActivity(AppCompatActivity activity, String title)
    {
        // Create a TextView programmatically.
        TextView tv = new TextView(activity.getApplicationContext());
        // Create a LayoutParams for TextView
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, // Width of TextView
                RelativeLayout.LayoutParams.WRAP_CONTENT); // Height of TextView
        // Apply the layout parameters to TextView widget
        tv.setLayoutParams(lp);
        // Set text to display in TextView
        tv.setText(title);
        // Set the text color of TextView
        tv.setTextColor(Color.WHITE);
        // Center align the ActionBar title
        tv.setGravity(Gravity.LEFT);
        // Set the serif font for TextView text
        // This will change ActionBar title text font
        tv.setTypeface(Typeface.SERIF, Typeface.ITALIC);
        // Set the ActionBar title font size
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,16);
        // Set the ActionBar display option
        activity.getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        // Finally, set the newly created TextView as ActionBar custom view
        activity.getSupportActionBar().setCustomView(tv);
    }

    public static boolean checkAndRequestPermissionForCamera(AppCompatActivity activity) {

        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.CAMERA},
                                                        CovidPasScannerApplication.PERMISSION_CAMERA);
        }
        return false;
    }

    public static boolean checkAndRequestPermissionForStorage(AppCompatActivity activity) {

        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                                                        CovidPasScannerApplication.PERMISSION_STORAGE);
        }
        return false;
    }
*/
}

/*
 syncTask.execute(new Runnable() {
@Override
public void run() {
        //TODO your background code
        }
        });


*/