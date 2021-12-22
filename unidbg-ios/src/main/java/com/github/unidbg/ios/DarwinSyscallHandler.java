package com.github.unidbg.ios;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.ios.DarwinFileIO;
import com.github.unidbg.file.ios.IOConstants;
import com.github.unidbg.ios.struct.VMStatistics;
import com.github.unidbg.ios.struct.kernel.HostStatisticsReply;
import com.github.unidbg.ios.struct.kernel.HostStatisticsRequest;
import com.github.unidbg.ios.struct.kernel.MachMsgHeader;
import com.github.unidbg.ios.struct.kernel.StatFS;
import com.github.unidbg.ios.struct.kernel.VprocMigLookupData;
import com.github.unidbg.ios.struct.kernel.VprocMigLookupReply;
import com.github.unidbg.ios.struct.kernel.VprocMigLookupRequest;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.pointer.UnidbgStructure;
import com.github.unidbg.spi.SyscallHandler;
import com.github.unidbg.unix.UnixEmulator;
import com.github.unidbg.unix.UnixSyscallHandler;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;

public abstract class DarwinSyscallHandler extends UnixSyscallHandler<DarwinFileIO> implements SyscallHandler<DarwinFileIO>, DarwinSyscall  {

    private static final Log log = LogFactory.getLog(DarwinSyscallHandler.class);

    final long bootTime = System.currentTimeMillis();

    /**
     * sysctl hw.machine
     */
    protected String getHwMachine() {
        return "iPhone6,2";
    }

    /**
     * sysctl hw.ncpu
     */
    protected int getHwNcpu() {
        return 2;
    }

    /**
     * sysctl kern.boottime
     */
    protected abstract void fillKernelBootTime(Pointer buffer);

    protected final void exit(Emulator<?> emulator) {
        RegisterContext context = emulator.getContext();
        int status = context.getIntArg(0);
        System.exit(status);
    }

    protected final int open_NOCANCEL(Emulator<DarwinFileIO> emulator, int offset) {
        RegisterContext context = emulator.getContext();
        Pointer pathname_p = context.getPointerArg(offset);
        int oflags = context.getIntArg(offset + 1);
        int mode = context.getIntArg(offset + 2);
        String pathname = pathname_p.getString(0);
        int fd = open(emulator, pathname, oflags);
        if (log.isDebugEnabled()) {
            log.debug("open_NOCANCEL pathname=" + pathname + ", oflags=0x" + Integer.toHexString(oflags) + ", mode=" + Integer.toHexString(mode) + ", fd=" + fd + ", LR=" + context.getLRPointer());
        }
        return fd;
    }

    protected int getfsstat64(Emulator<DarwinFileIO> emulator, int off) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer buf = context.getPointerArg(off);
        int bufSize = context.getIntArg(off + 1);
        int flags = context.getIntArg(off + 2);
        if (log.isDebugEnabled()) {
            log.debug("getfsstat64 buf=" + buf + ", bufSize=" + bufSize + ", flags=0x" + Integer.toHexString(flags));
        }

        final int mountedFsSize = 2;
        if (buf == null) {
            return mountedFsSize;
        }

        buf.setSize(bufSize);
        Pointer pointer = buf;
        int statfs_size = UnidbgStructure.calculateSize(StatFS.class);

        if (bufSize >= statfs_size) {
            StatFS statFS = new StatFS(pointer);
            statFS.f_bsize = 0x1000;
            statFS.f_iosize = 0x100000;
            statFS.f_blocks = 507876;
            statFS.f_bfree = 76016;
            statFS.f_bavail = 70938;
            statFS.f_files = 507874;
            statFS.f_ffree = 70938;
            statFS.f_fsid = 0x1101000002L;
            statFS.f_owner = 0;
            statFS.f_type = 0x11;
            statFS.f_flags = 0x480d000;
            statFS.f_fssubtype = 0x3;
            statFS.setFsTypeName("hfs");
            statFS.setMntOnName("/");
            statFS.setMntFromName("/dev/disk0s1s1");
            statFS.pack();

            bufSize -= statfs_size;
            pointer = pointer.share(statfs_size);
        }

        if (bufSize >= statfs_size) {
            StatFS statFS = new StatFS(pointer);
            statFS.f_bsize = 0x1000;
            statFS.f_iosize = 0x100000;
            statFS.f_blocks = 3362844;
            statFS.f_bfree = 3000788;
            statFS.f_bavail = 3000788;
            statFS.f_files = 3362842;
            statFS.f_ffree = 3000788;
            statFS.f_fsid = 0x1101000003L;
            statFS.f_owner = 0;
            statFS.f_type = 0x11;
            statFS.f_flags = 0x14809080;
            statFS.f_fssubtype = 0x3;
            statFS.setFsTypeName("hfs");
            statFS.setMntOnName("/private/var");
            statFS.setMntFromName("/dev/disk0s1s2");
            statFS.pack();
        }
        if (verbose) {
            System.out.printf("getfsstat from %s%n", emulator.getContext().getLRPointer());
        }

        return mountedFsSize;
    }

    protected final int access(Emulator<DarwinFileIO> emulator) {
        RegisterContext context = emulator.getContext();
        Pointer pathname = context.getPointerArg(0);
        int mode = context.getIntArg(1);
        String path = pathname.getString(0);
        if (log.isDebugEnabled()) {
            log.debug("access pathname=" + path + ", mode=" + mode);
        }
        return faccessat(emulator, path, mode);
    }

    protected final int faccessat(Emulator<DarwinFileIO> emulator, String pathname, int mode) {
        FileResult<?> result = resolve(emulator, pathname, IOConstants.O_RDONLY);
        if (result != null && result.isSuccess()) {
            if (verbose) {
                System.out.printf("File access '%s' with mode=0x%x from %s%n", pathname, mode, emulator.getContext().getLRPointer());
            }
            return 0;
        }

        emulator.getMemory().setErrno(result != null ? result.errno : UnixEmulator.ENOENT);
        if (verbose) {
            System.out.printf("File access failed '%s' with mode=0x%x from %s%n", pathname, mode, emulator.getContext().getLRPointer());
        }
        return -1;
    }

    protected final int listxattr(Emulator<DarwinFileIO> emulator) {
        RegisterContext context = emulator.getContext();
        Pointer path = context.getPointerArg(0);
        UnidbgPointer namebuf = context.getPointerArg(1);
        int size = context.getIntArg(2);
        int options = context.getIntArg(3);
        String pathname = path.getString(0);
        FileResult<DarwinFileIO> result = resolve(emulator, pathname, IOConstants.O_RDONLY);
        if (namebuf != null) {
            namebuf.setSize(size);
        }
        if (result.isSuccess()) {
            int ret = result.io.listxattr(namebuf, size, options);
            if (ret == -1) {
                log.info("listxattr path=" + pathname + ", namebuf=" + namebuf + ", size=" + size + ", options=" + options + ", LR=" + context.getLRPointer());
            } else {
                if (log.isDebugEnabled()) {
                    log.info("listxattr path=" + pathname + ", namebuf=" + namebuf + ", size=" + size + ", options=" + options + ", LR=" + context.getLRPointer());
                }
            }
            return ret;
        } else {
            log.info("listxattr path=" + pathname + ", namebuf=" + namebuf + ", size=" + size + ", options=" + options + ", LR=" + context.getLRPointer());
            emulator.getMemory().setErrno(UnixEmulator.ENOENT);
            return -1;
        }
    }

    protected final int chmod(Emulator<DarwinFileIO> emulator) {
        RegisterContext context = emulator.getContext();
        Pointer path = context.getPointerArg(0);
        int mode = context.getIntArg(1) & 0xffff;
        String pathname = path.getString(0);
        FileResult<DarwinFileIO> result = resolve(emulator, pathname, IOConstants.O_RDONLY);
        if (result.isSuccess()) {
            int ret = result.io.chmod(mode);
            if (ret == -1) {
                log.info("chmod path=" + pathname + ", mode=0x" + Integer.toHexString(mode));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("chmod path=" + pathname + ", mode=0x" + Integer.toHexString(mode));
                }
            }
            return ret;
        } else {
            log.info("chmod path=" + pathname + ", mode=0x" + Integer.toHexString(mode));
            emulator.getMemory().setErrno(UnixEmulator.ENOENT);
            return -1;
        }
    }

    protected final boolean host_statistics(Pointer request, MachMsgHeader header) {
        HostStatisticsRequest args = new HostStatisticsRequest(request);
        args.unpack();
        if (log.isDebugEnabled()) {
            log.debug("host_statistics args=" + args);
        }

        if (args.flavor == HostStatisticsRequest.HOST_VM_INFO) {
            int size = UnidbgStructure.calculateSize(VMStatistics.class);
            HostStatisticsReply reply = new HostStatisticsReply(request, size);
            reply.unpack();

            header.setMsgBits(false);
            header.msgh_size = header.size() + reply.size();
            header.msgh_remote_port = header.msgh_local_port;
            header.msgh_local_port = 0;
            header.msgh_id += 100; // reply Id always equals reqId+100
            header.pack();

            reply.writeVMStatistics();
            reply.retCode = 0;
            reply.host_info_outCnt = size / 4;
            reply.pack();

            if (log.isDebugEnabled()) {
                log.debug("host_statistics HOST_VM_INFO reply=" + reply);
            }
            return true;
        }

        return false;
    }

    final int vproc_mig_look_up2(Pointer request, MachMsgHeader header) {
        VprocMigLookupRequest args = new VprocMigLookupRequest(request);
        args.unpack();
        String serviceName = args.getServiceName();
        if (log.isDebugEnabled()) {
            log.debug("vproc_mig_look_up2 args=" + args + ", serviceName=" + serviceName);
        }

        if ("cy:com.saurik.substrated".equals(serviceName)) {
            return -1;
        }

        VprocMigLookupReply reply = new VprocMigLookupReply(request);
        reply.unpack();

        header.msgh_bits = (header.msgh_bits & 0xff) | MACH_MSGH_BITS_COMPLEX;
        header.msgh_size = header.size() + reply.size();
        header.msgh_remote_port = header.msgh_local_port;
        header.msgh_local_port = 0;
        header.msgh_id += 100; // reply Id always equals reqId+100
        header.pack();

        reply.body.msgh_descriptor_count = 1;
        reply.sp.name = STATIC_PORT;
        reply.sp.pad1 = 0;
        reply.sp.pad2 = 0;
        reply.sp.disposition = 17;
        reply.sp.type = MACH_MSG_PORT_DESCRIPTOR;
        reply.pack();

        VprocMigLookupData data = new VprocMigLookupData(request.share(reply.size()));
        data.size = 0x20;
        Arrays.fill(data.au_tok.val, 0);
        data.pack();

        if (log.isDebugEnabled()) {
            log.debug("vproc_mig_look_up2 reply=" + reply + ", data=" + data);
        }
        return MACH_MSG_SUCCESS;
    }

    protected String executableBundlePath;

    public void setExecutableBundlePath(String executableBundlePath) {
        this.executableBundlePath = executableBundlePath;
    }

}
