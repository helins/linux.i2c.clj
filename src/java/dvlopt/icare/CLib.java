package dvlopt.icare ;


import com.sun.jna.Native ;
import com.sun.jna.NativeLong ;


public class CLib {

    public static native int ioctl( int        fd      ,
                                    NativeLong request ,
                                    long       arg     ) ;


    static {
        Native.register( "c" ) ;
    }
}
