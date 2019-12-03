package cardInfoApplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.ISO7816;
import javacard.framework.Util;
import javacard.framework.JCSystem;


/**
 * Simple applet for sending memory information apdus
 *
 * @author Kristian Mika
 */
public class GetCardInfo extends Applet {
    public static final byte CLA_INFO = (byte) 0x31;
    public static final byte GET_INFO = (byte) 0x41;


    protected GetCardInfo(byte[] bArray, short bOffset, byte bLength) {
        register();
    }


    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
        new GetCardInfo(bArray, bOffset, bLength);
    }

    @Override
    public boolean select() {
        return true;
    }


    @Override
    public void process(APDU apdu) throws ISOException {
        apdu.setIncomingAndReceive();
        byte[] apduBuffer = apdu.getBuffer();
        if (selectingApplet()) {
            return;
        }

        if (apduBuffer[ISO7816.OFFSET_CLA] == CLA_INFO) {
            switch (apduBuffer[ISO7816.OFFSET_INS]) {
                case GET_INFO:
                    getCardInfo(apdu);
                    break;
                default:
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }

        } else {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }


    /**
     * Writes the memory info to the 'apdu' packet and sends it
     *
     * @param apdu received packet
     */
    public void getCardInfo(APDU apdu) {
        byte[] apduBuffer = apdu.getBuffer();
        short offset = 0;
        Util.setShort(apduBuffer, offset, JCSystem.getAvailableMemory(JCSystem.MEMORY_TYPE_PERSISTENT));
        offset += 2;
        Util.setShort(apduBuffer, offset, JCSystem.getAvailableMemory(JCSystem.MEMORY_TYPE_TRANSIENT_RESET));
        offset += 2;
        Util.setShort(apduBuffer, offset, JCSystem.getAvailableMemory(JCSystem.MEMORY_TYPE_TRANSIENT_DESELECT));
        offset += 2;
        apdu.setOutgoingAndSend((short) 0, offset);
    }


}
