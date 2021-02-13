/**
 * ATM.java: collection of constants used in the
 * UBERPUTER ATM SPECIFICATION REV.1 DRAFT (marked
 * with "PROTOCOL", and some other constants used
 * by the reference server implementation.
 * 
 * @author Mikael Forsberg <miforsb@kth.se>
 * @author Robin Gunning <rgunning@kth.se>
 * @version 20151204T1700
 */
public abstract class ATM
{
    /**
     * PROTOCOL
     * MENU REQUEST, client-to-server
     */
    public static final int MSG_MENU_REQUEST = 0;
    
    /**
     * PROTOCOL
     * MENU NUM ITEMS, server-to-client
     */
    public static final int MSG_MENU_NUM_ITEMS = 1;
    
    /**
     * PROTOCOL
     * ACTION, client-to-server
     */
    public static final int MSG_ACTION = 2;
    
    /**
     * PROTOCOL
     * UPDATE REQUEST, client-to-server
     */
    public static final int MSG_UPDATE_REQUEST = 3;
    
    /**
     * PROTOCOL
     * MENU ITEM, server-to-client
     */
    public static final int MSG_MENU_ITEM = 4;
    
    /**
     * PROTOCOL
     * OK, server-to-client
     */
    public static final int MSG_OK = 5;
    
    /**
     * PROTOCOL
     * FAIL, server-to-client
     */
    public static final int MSG_FAIL = 6;
    
    /**
     * PROTOCOL
     * UPDATE, server-to-client
     */
    public static final int MSG_UPDATE = 7;
    
    /**
     * PROTOCOL
     * RESPONSE, server-to-client
     */
    public static final int MSG_RESPONSE = 8;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 0 is reserved
     */
    public static final int TYPE_RES1 = 1;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 1 is reserved
     */
    public static final int TYPE_RES2 = 2;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 2 is reserved
     */
    public static final int TYPE_RES3 = 4;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 3 is reserved
     */
    public static final int TYPE_RES4 = 8;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 4 is reserved
     */
    public static final int TYPE_RES5 = 16;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 5 indicates that a client should,
     * AFTER executing an action where this type is present,
     * receive a follow-up MENU ITEM containing an action to
     * execute immediately.
     */
    public static final int TYPE_RECV_FOLLOWUP = 32;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 6 indicates that a client should,
     * AFTER executing an action where this type is present,
     * receive a RESPONSE with a 32-bit unsigned integer in
     * the "response" field.
     */
    public static final int TYPE_RECV_UINT32 = 64;
    
    /**
     * PROTOCOL
     * Type bitfield, bit 7 indicates that a client should,
     * WHEN executing an action where this type is present,
     * set the value of the "arg" field to a 32-bit unsigned integer.
     */
    public static final int TYPE_SND_UINT32 = 128;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_BALANCE = 0;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_DEPOSIT = 1;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_WITHDRAW = 2;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_UPDATE_REQUEST = 3;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_LOGIN_CARDNO = 5;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_LOGIN_PIN = 6;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_LOGOUT = 7;
    
    /**
     * Internal enumeration of an action used by the server.
     */
    public static final int ACT_OTP_WITHDRAW = 8;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_NONE = 0;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_BANNER = 1;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_BALANCE = 2;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_DEPOSIT = 3;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_WITHDRAW = 4;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_BALANCETEXT = 5;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_DEPOSITTEXT = 6;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_WITHDRAWTEXT = 7;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_LOGIN = 8;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_ERRORTEXT = 9;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_AMNTERROR = 10;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_LOGOUT = 11;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_LOGOUTTEXT = 12;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_LOGINTEXT_CARD = 13;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_LOGINTEXT_PIN = 14;
    
    /**
     * Internal enumeration of a multi-language string used by the server.
     */
    public static final int S_OTPTEXT = 15;
}
