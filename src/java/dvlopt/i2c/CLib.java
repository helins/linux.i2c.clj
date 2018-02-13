package dvlopt.i2c ;


import com.sun.jna.Native ;
import com.sun.jna.NativeLong ;


public class CLib {

    public static native int ioctl( int        fd      ,
                                    NativeLong request ,
                                    long       arg     ) ; // TODO Should `arg` be long ?


    static {
        Native.register( "c" ) ;
    }
}
