package cardInfoPCClient;

import javacard.framework.ISO7816;
import javacard.framework.Util;

import javax.smartcardio.*;
import java.security.InvalidParameterException;

/**
 * Represents a PC client for acquiring card memory information
 *
 * @author Kristian Mika
 */
public class PCClient {
    public static final byte CLA_INFO = (byte) 0x31;
    public static final byte GET_INFO = (byte) 0x41;
    private static final byte[] appletAID = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, (byte) 0x0a, (byte) 0x32,
            (byte) 0xa1, (byte) 0xa3, (byte) 0x4e, (byte) 0x13, (byte) 0x22, (byte) 0x1E, (byte) 0x12, (byte) 0x33, (byte) 0x19};
    private static Boolean debug = true;
    CardChannel channel;

    /**
     * Connects to a single card and sends getCardInfo request
     *
     * @param args unused
     * @throws CardException if getCardInfo fails
     */
    public static void main(String[] args) throws CardException {
        if (debug) {
            System.out.println("[INFO] Debug mode active");
        }
        PCClient client = new PCClient();
        client.channel = connectToCardByTerminalFactory(TerminalFactory.getDefault());
        System.out.println("............................................");

        getCardInfo(true, client);

    }

    /**
     * Sends apdu request for memory info
     *
     * @param printInfo if true prints results
     * @param client    that sends the apdu
     * @return response as byte array
     * @throws CardException is apdu transmission fails
     */
    private static byte[] getCardInfo(Boolean printInfo, PCClient client) throws CardException {
        CommandAPDU cmdToBeSent = new CommandAPDU(CLA_INFO, GET_INFO, 0, 0);
        if (debug) {
            System.out.println("[->] Sending GetInfo request...");
        }
        byte[] response = transmit(client.channel, cmdToBeSent).getBytes();
        if (printInfo) {
            short offset = 0;
            System.out.println("Available persistent memory:         " + Util.getShort(response, offset) +
                    "B (" + Util.getShort(response, offset) / 1024 + " kB)");
            offset += 2;
            System.out.println("Available transient reset memory:    " + Util.getShort(response, offset) +
                    "B (" + Util.getShort(response, offset) / 1024 + " kB)");
            offset += 2;
            System.out.println("Available transient deselect memory: " + Util.getShort(response, offset) +
                    "B (" + Util.getShort(response, offset) / 1024 + " kB)");
        }
        return response;
    }


    /**
     * Connects to a single card using terminal factory
     *
     * @param factory used for connecting
     * @return card channel is connected successfully
     * @throws CardException if applet selection fails
     */
    private static CardChannel connectToCardByTerminalFactory(TerminalFactory factory) throws CardException {
        CardTerminal terminal = null;
        if (debug) {
            System.out.println("[->] Connecting to cards...");
        }
        try {
            terminal = factory.terminals().list().get(0);
        } catch (CardException e) {
            throw new InvalidParameterException("[ERROR] No cards found");
        }
        if (debug) {
            System.out.println("[OK] Connection successful");
        }
        Card card = terminal.connect("*");
        System.out.println("[->] Selecting GetCardInfo applet...");

        CardChannel channel = card.getBasicChannel();
        CommandAPDU cmd = new CommandAPDU(appletAID);

        ResponseAPDU response = transmit(channel, cmd);
        if (checkResponse(response)) {
            System.out.println("[OK] Applet has been selected successfully.");
        }
        return card.getBasicChannel();
    }

    /**
     * Transmits CommandAPDU via 'channel' CardChannel
     *
     * @param channel used for transmitting cmds
     * @param cmd     to be transmitted
     * @return response APDU
     * @throws CardException if transmission fails
     */
    public static ResponseAPDU transmit(CardChannel channel, CommandAPDU cmd) throws CardException {
        if (debug) {
            System.out.print("-> ");
            for (byte apduByte : cmd.getBytes()) {
                System.out.print(String.format("%02x ", apduByte));
            }
        }
        ResponseAPDU response = channel.transmit(cmd);
        assert checkResponse(response);
        if (debug) {
            System.out.print("\n<- ");
            for (byte apduByte : response.getBytes()) {
                System.out.print(String.format("%02x ", apduByte));
            }
            System.out.println();
        }
        return response;
    }

    /**
     * Checks response APDU code
     *
     * @param response from the card
     * @return true if response code was ISO7816.SW_NO_ERROR, false otherwise
     */
    public static boolean checkResponse(ResponseAPDU response) {
        if (response.getSW() != (ISO7816.SW_NO_ERROR & 0xffff)) {
            System.err.println(String.format("[ERROR] Received error status: %02X.", response.getSW()));
            return false;
        }
        return true;
    }
}
