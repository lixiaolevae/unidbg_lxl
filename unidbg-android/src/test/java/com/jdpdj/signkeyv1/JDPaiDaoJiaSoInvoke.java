package com.jdpdj.signkeyv1;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class JDPaiDaoJiaSoInvoke extends AbstractJni {
    private final AndroidEmulator emulator;
    private final Module module;
    private final VM vm;

    public String apkPath = "unidbg-android/src/test/resources/120_8ac62a329308615bc4333c9a46ac398a.apk";
    public String soPath = "unidbg-android/src/test/resources/libjdpdj.so";

    private static LibraryResolver createLibraryResolver() {
        return new AndroidResolver(23); //TODO
    }

    private static AndroidEmulator createARMEmulator() {
        return AndroidEmulatorBuilder.for32Bit().build();   //TODO
    }

    public JDPaiDaoJiaSoInvoke() {
        emulator = createARMEmulator();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(createLibraryResolver());
        vm = emulator.createDalvikVM(new File(apkPath));
        vm.setVerbose(true);

        DalvikModule dm = vm.loadLibrary(new File(soPath), false);
        vm.setJni(this);

        dm.callJNI_OnLoad(emulator);
        module = dm.getModule();

    }

    public void getSignKeyV1(String str) throws UnsupportedEncodingException {
        DvmClass zClass = vm.resolveClass("jd/net/z");
        DvmObject<?> strRc = zClass.callStaticJniMethodObject(emulator
                , "k2([B)Ljava/lang/String;"
                , new ByteArray(vm, "参数".getBytes("UTF-8"))
        );

//        Object value = strRc.getValue();
        System.out.println(" callGetSignKeyV1 = " + strRc.getValue());
    }


    //FIXME WARN [com.github.unidbg.linux.ARM32SyscallHandler] (ARM32SyscallHandler:528) - handleInterrupt intno=2, NR=-1073744088, svcNumber=0x16e, PC=unidbg@0xfffe0774, LR=RX@0x40035bdd[libjdpdj1.so]0x35bdd, syscall=null
    //FIXME java.lang.UnsupportedOperationException: jd/utils/StatisticsReportUtil->getSign()Ljava/lang/String;
    @Override
    public DvmObject<?> callStaticObjectMethod(BaseVM vm, DvmClass dvmClass, String signature, VarArg varArg) {
        if("jd/utils/StatisticsReportUtil->getSign()Ljava/lang/String;".equals(signature)) {
            return new StringObject(vm, "30819f300d06092a864886f70d010101050003818d00308189028181008c470af7c751ee12edbae8dd9e7c98fa60d3c631efa0f7172ed36c86bb85c8288391e718c05fdbef008d61f2e8fce4ef4457a69ae5a2fa53ead0c806c18f8b475847c07bf4451d82845efc30d5fc4aa2500f4bc84234a36749e83a9361c9ec89771a762e3d791eebf3154c2e95d06df95be68b4a4dcff33ef1ba5d6d90758b6d0203010001");
        }
        return super.callStaticObjectMethod(vm, dvmClass, signature, varArg);
    }

    public void destroy() throws IOException {
        emulator.close();
    }

    public static void main(String[] args) throws IOException {
        JDPaiDaoJiaSoInvoke invoker = new JDPaiDaoJiaSoInvoke();
        invoker.getSignKeyV1("参数");
        invoker.destroy();
    }





}
