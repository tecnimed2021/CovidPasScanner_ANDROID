package it.tecnimed.covidpasscanner.uart;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by lucavaltellina on 28/04/16.
 */
public class UARTDriver implements SerialInputOutputManager.Listener
{
    private static final String TAG_LOG = UARTDriver.class.getName();
    private static final int BUF_SIZE = 4096;
    public static final int UARTDRIVER_PORT_MODE_NOEVENT = 0;
    public static final int UARTDRIVER_PORT_MODE_EVENTONREAD = 1;
    public static final int UARTDRIVER_STOPBIT_1 = UsbSerialPort.STOPBITS_1;
    public static final int UARTDRIVER_STOPBIT_1_5 = UsbSerialPort.STOPBITS_1_5;
    public static final int UARTDRIVER_STOPBIT_2 = UsbSerialPort.STOPBITS_2;
    public static final int UARTDRIVER_PARITY_NONE = UsbSerialPort.PARITY_NONE;
    public static final int UARTDRIVER_PARITY_ODD = UsbSerialPort.PARITY_ODD;
    public static final int UARTDRIVER_PARITY_EVEN = UsbSerialPort.PARITY_EVEN;

    private static Context mContext;
    private SerialInputOutputManager mSerialIoManager;
    private UsbSerialDriver mDriver;
    private UsbDeviceConnection mConnection;
    private int mPortMode;
    private UsbSerialPort mPort;
    private byte[] bufR = new byte[BUF_SIZE];
    private int bufRIn = 0, bufROut = 0;
    private int idcnt = 0;





    // Costruttore
    public UARTDriver(Context ctx)
    {
        mContext = ctx;
        mDriver = null;
        mConnection = null;
        mPortMode = UARTDRIVER_PORT_MODE_NOEVENT;
        mPort = null;
    }

    public static UARTDriver create(Context ctx)
    {
        final UARTDriver obj = new UARTDriver(ctx);
        return obj;
    }

    public boolean init()
    {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return false;
        }
        // Open a connection to the first available driver.
        mDriver = availableDrivers.get(0);
        mConnection = manager.openDevice(mDriver.getDevice());
        if (mConnection == null) {
/*            if (ContextCompat.checkSelfPermission(mContext,  com.google.android.things.permission.USE_PERIPHERAL_IO) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(mContext, new String[] {Manifest.permission.}, ENFProApplication.PERMISSION_CAMERA);
            }
*/            // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
            return false;
        }

        return true;
    }

    public boolean openPort(int mode, int portn, int baud, int stopbit, int parity)
    {
        mPortMode = mode;
        mPort = mDriver.getPorts().get(portn); // Most devices have just one port (port 0)
        try {
            mPort.open(mConnection);
            mPort.setParameters(baud, 8, stopbit, parity);
            if(mPortMode == UARTDRIVER_PORT_MODE_EVENTONREAD) {
                mSerialIoManager = new SerialInputOutputManager(mPort, this);
                Executors.newSingleThreadExecutor().submit(mSerialIoManager);
            }
        }
        catch (IOException e) {
            Log.w(TAG_LOG, "Unable to open UART device", e);
            return false;
        }
        return true;
    }

    public void closePort() {
        try {
            mPort.close();
        }
        catch (IOException e) {
            Log.w(TAG_LOG, "Unable to close UART device", e);
        }
    }

    public int read(byte[] buf, int timeout)
    {
        int n = 0;

        if (mPortMode == UARTDRIVER_PORT_MODE_NOEVENT) {
            try {
                n = mPort.read(buf, timeout);
            } catch (IOException e) {
                n = 0;
                Log.w(TAG_LOG, "Unable to read UART device", e);
            }
        } else if (mPortMode == UARTDRIVER_PORT_MODE_EVENTONREAD) {
            n = 0;
            while (true) {
                if (bufROut == bufRIn)
                    break;
                else {
                    if (bufROut >= BUF_SIZE)
                        bufROut = 0;
                    buf[n] = bufR[bufROut];
                    bufROut++;
                    n++;
                }
            }
        }

        return n;
    }

    public int write(byte[] buf, int size)
    {
        int n = 0;
        try {
            byte[] b = new byte[size];
            for(int i=0; i<size; i++) {
                b[i] = buf[i];
            }
            n = mPort.write(b, 100);
        } catch (IOException e) {
            n = 0;
            Log.w(TAG_LOG, "Unable to write UART device", e);
        }

        return n;
    }
/*
    public int write(byte[] buf, int size)
    {
        int n = 0;
        byte[] b = new byte[1];
        try {
            for(int i=0; i<size; i++) {
                b[0] = buf[i];
                n = mPort.write(b, 1);
            }
        } catch (IOException e) {
            n = 0;
            Log.w(TAG_LOG, "Unable to write UART device", e);
        }

        return n;
    }
*/
    public int flush()
    {
        try {
            mPort.purgeHwBuffers(true, true);
        } catch (IOException e) {
            Log.w(TAG_LOG, "Unable to flush Read UART device", e);
        }

        return 0;
    }

    // SerialInputOutputManager listener
    @Override
    public void onRunError(Exception e) {
        Log.d(TAG_LOG, "Runner stopped.");
    }

    @Override
    public void onNewData(final byte[] data)
    {
        for(int i=0; i<data.length; i++)
        {
            if(bufRIn >= BUF_SIZE)
                bufRIn = 0;
            bufR[bufRIn] = data[0];
            bufRIn++;
        }
    }
}
