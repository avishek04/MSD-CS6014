"""
Adapted from https://github.com/sholsapp/fusepy-base/blob/master/fusepybase/passthrough.py
WHo provided a naive passthrough FUSE filesystem.

You'll need to modify the constructor (__init__), open(), create(), 
read(), write(), truncate(), and release() methods.  See the assignment description for
what to do

Note, the documentation present here is documentation for the C FUSE API, and
may not correspond 1-to-1 with the Python FUSE API. It is included for
informational purposes only to highlight the intended purpose of each method.

"""

from __future__ import with_statement

from functools import wraps
import getpass
import os
import sys
import errno
import logging

from fuse import FUSE, FuseOSError, Operations
import inspect


import base64
from cryptography.fernet import Fernet
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

log = logging.getLogger(__name__)


def logged(f):
    @wraps(f)
    def wrapped(*args, **kwargs):
        log.info('%s(%s)', f.__name__, ','.join([str(item) for item in args[1:]]))
        return f(*args, **kwargs)
    return wrapped



class EncFS(Operations):
    """A simple passthrough interface.

    Initialize the filesystem. This function can often be left unimplemented, but
    it can be a handy way to perform one-time setup such as allocating
    variable-sized data structures or initializing a new filesystem. The
    fuse_conn_info structure gives information about what features are supported
    by FUSE, and can be used to request certain capabilities (see below for more
    information). The return value of this function is available to all file
    operations in the private_data field of fuse_context. It is also passed as a
    parameter to the destroy() method.

    """

    def __init__(self, root):
        self.root = root
        self.openFiles = {}
        self.netFD = 3
        self.nextFD = 0
        #TODO, FINISH ME!!
        self.password = getpass.getpass()
    
    def destroy(self, path):
        """Clean up any resources used by the filesystem.
        
        Called when the filesystem exits.
        
        """
        pass

    #NOTE THIS MIGHT BE USEFUL IN SEVERAL PLACES!
    def _full_path(self, partial):
        """Calculate full path for the mounted file system.

          .. note::

            This isn't the same as the full path for the underlying file system.
            As such, you can't use os.path.abspath to calculate this, as that
            won't be relative to the mount point root.

        """
        if partial.startswith("/"):
            partial = partial[1:]
        path = os.path.join(self.root, partial)
        return path

    @logged
    def access(self, path, mode):
        """Access a file.

        This is the same as the access(2) system call. It returns -ENOENT if
        the path doesn't exist, -EACCESS if the requested permission isn't
        available, or 0 for success. Note that it can be called on files,
        directories, or any other object that appears in the filesystem. This
        call is not required but is highly recommended.

        """
        full_path = self._full_path(path)
        if not os.access(full_path, mode):
            raise FuseOSError(errno.EACCES)

    @logged
    def chmod(self, path, mode):
        """Change a file's permissions.

        Change the mode (permissions) of the given object to the given new
        permissions. Only the permissions bits of mode should be examined. See
        chmod(2) for details.

        """
        full_path = self._full_path(path)
        return os.chmod(full_path, mode)

    @logged
    def chown(self, path, uid, gid):
        """Change a file's owernship.


        Change the given object's owner and group to the provided values. See
        chown(2) for details. NOTE: FUSE doesn't deal particularly well with
        file ownership, since it usually runs as an unprivileged user and this
        call is restricted to the superuser. It's often easier to pretend that
        all files are owned by the user who mounted the filesystem, and to skip
        implementing this function.

        """
        full_path = self._full_path(path)
        return os.chown(full_path, uid, gid)

    @logged
    def getattr(self, path, fh=None):
        """Return file attributes.

        The "stat" structure is described in detail in the stat(2) manual page.
        For the given pathname, this should fill in the elements of the "stat"
        structure. If a field is meaningless or semi-meaningless (e.g., st_ino)
        then it should be set to 0 or given a "reasonable" value. This call is
        pretty much required for a usable filesystem.

        """
        fullPath = self._full_path(path)
        st = os.lstat(fullPath)
        
        # print("FIXME FIXME: update so that the size reported here is the length of the decrypted data, not the physical file size!")
        
        props = dict((key, getattr(st, key)) for key in ('st_atime', 'st_ctime',
               'st_gid', 'st_mode', 'st_mtime', 'st_nlink', 'st_size', 'st_uid'))

        '''TODO FINISH ME TO UPDATE THE st_size fiel to the size of the unencrypted content'''

        length = 0
        if (not os.path.isdir(path) or not os.path.isdir(fullPath)):
            if not os.path.exists(fullPath):
                return -1
            
            if (fullPath not in self.openFiles):
                file = os.open(fullPath, os.O_RDONLY)
                salt = os.read(file, 16)
                encrypt = os.read(file, os.path.getsize(fullPath))
                kdf = PBKDF2HMAC(
                    algorithm=hashes.SHA256(),
                    length=32,
                    salt=salt,
                    iterations=480000,
                )
                key = base64.urlsafe_b64encode(kdf.derive(str.encode(self.password)))
                f = Fernet(key)
                length = len(f.decrypt(encrypt))
                os.close(file)
            elif (not self.openFiles[fullPath] is None):
                length = len(self.openFiles[fullPath])
                     
            props['st_size'] = length
        
        return props

    @logged
    def readdir(self, path, fh):
        """Read a directory.

        Return one or more directory entries (struct dirent) to the caller.
        This is one of the most complex FUSE functions. It is related to, but
        not identical to, the readdir(2) and getdents(2) system calls, and the
        readdir(3) library function. Because of its complexity, it is described
        separately below. Required for essentially any filesystem, since it's
        what makes ls and a whole bunch of other things work.

        """
        full_path = self._full_path(path)

        dirents = ['.', '..']
        if os.path.isdir(full_path):
            dirents.extend(os.listdir(full_path))
        for r in dirents:
            yield r

    @logged
    def readlink(self, path):
        """Read a symbolic link.

        If path is a symbolic link, fill buf with its target, up to size. See
        readlink(2) for how to handle a too-small buffer and for error codes.
        Not required if you don't support symbolic links. NOTE: Symbolic-link
        support requires only readlink and symlink. FUSE itself will take care
        of tracking symbolic links in paths, so your path-evaluation code
        doesn't need to worry about it.

        """
        pathname = os.readlink(self._full_path(path))
        if pathname.startswith("/"):
            # Path name is absolute, sanitize it.
            return os.path.relpath(pathname, self.root)
        else:
            return pathname

        #don't allow mknod
    '''@logged
    def mknod(self, path, mode, dev):
        """Make a special file.

        Make a special (device) file, FIFO, or socket. See mknod(2) for
        details. This function is rarely needed, since it's uncommon to make
        these objects inside special-purpose filesystems.

        """
        return os.mknod(self._full_path(path), mode, dev)
'''
    @logged
    def rmdir(self, path):
        """Remove a directory.

        Remove the given directory. This should succeed only if the directory
        is empty (except for "." and ".."). See rmdir(2) for details.

        """
        full_path = self._full_path(path)
        return os.rmdir(full_path)

    @logged
    def mkdir(self, path, mode):
        """Make a directory.

        Create a directory with the given name. The directory permissions are
        encoded in mode. See mkdir(2) for details. This function is needed for
        any reasonable read/write filesystem.

        """
        return os.mkdir(self._full_path(path), mode)

    @logged
    def statfs(self, path):
        full_path = self._full_path(path)
        stv = os.statvfs(full_path)
        return dict((key, getattr(stv, key)) for key in ('f_bavail', 'f_bfree',
          'f_blocks', 'f_bsize', 'f_favail', 'f_ffree', 'f_files', 'f_flag',
          'f_frsize', 'f_namemax'))

    @logged
    def unlink(self, path):
        """Unlink a file.

        Remove (delete) the given file, symbolic link, hard link, or special
        node. Note that if you support hard links, unlink only deletes the data
        when the last hard link is removed. See unlink(2) for details.

        """
        return os.unlink(self._full_path(path))

    #no symlinks
    '''    @logged
    def symlink(self, target, name):
        """Create a symbolic link.

        Create a symbolic link named "from" which, when evaluated, will lead to
        "to". Not required if you don't support symbolic links. NOTE:
        Symbolic-link support requires only readlink and symlink. FUSE itself
        will take care of tracking symbolic links in paths, so your
        path-evaluation code doesn't need to worry about it.

        """
        return os.symlink(self._full_path(target), self._full_path(name))
    '''
    @logged
    def rename(self, old, new):
        """Rename a file.

        Rename the file, directory, or other object "from" to the target "to".
        Note that the source and target don't have to be in the same directory,
        so it may be necessary to move the source to an entirely new directory.
        See rename(2) for full details.

        """
        return os.rename(self._full_path(old), self._full_path(new))
#no hard links either
    '''
    @logged
    def link(self, target, name):
        """Create a hard link.

        Create a hard link between "from" and "to". Hard links aren't required
        for a working filesystem, and many successful filesystems don't support
        them. If you do implement hard links, be aware that they have an effect
        on how unlink works. See link(2) for details.

        """
        return os.link(self._full_path(target), self._full_path(name))
'''
    @logged
    def utimens(self, path, times=None):
        return os.utime(self._full_path(path), times)

    @logged
    def open(self, path, flags):
        """Open a file.

        Open a file. If you aren't using file handles, this function should
        just check for existence and permissions and return either success or
        an error code. If you use file handles, you should also allocate any
        necessary structures and set fi->fh. In addition, fi has some other
        fields that an advanced filesystem might find useful; see the structure
        definition in fuse_common.h for very brief commentary.

        """
        fullPath = self._full_path(path)
        if (not os.path.exists(fullPath) or path in self.openFiles):
            return -1
        else:
            file = os.open(fullPath, os.O_RDWR)
            salt = os.read(file, 16)
            kdf = PBKDF2HMAC(
                algorithm=hashes.SHA256(),
                length=32,
                salt=salt,
                iterations=480000)
            key = base64.urlsafe_b64encode(kdf.derive(str.encode(self.password)))
            f = Fernet(key)
            encrypt = os.read(file, os.path.getsize(fullPath))
            msg = f.decrypt(encrypt)
            self.openFiles[fullPath] = msg
            self.nextFD += 1
            os.close(file)
        return self.nextFD
        
    @logged
    def create(self, path, mode, fi=None):
        fullPath = self._full_path(path)
        if (os.path.exists(fullPath) or path in self.openFiles):
            return -1
        
        file = os.open(fullPath, os.O_WRONLY | os.O_CREAT, mode)
        # msg = b''
        self.openFiles[fullPath] = None
        os.close(file)
        self.nextFD += 1
        return self.nextFD

    @logged
    def read(self, path, length, offset, fh):
        """Read from a file.

        Read size bytes from the given file into the buffer buf, beginning
        offset bytes into the file. See read(2) for full details. Returns the
        number of bytes transferred, or 0 if offset was at or beyond the end of
        the file. Required for any sensible filesystem.

        """
        fullPath = self._full_path(path)

        if (fullPath not in self.openFiles):
            return -1
        
        start = offset
        end = offset + length

        # if (byteArr.len() <= offset + length):
        #     return byteArr[start:]

        return self.openFiles[fullPath][start:end]

    @logged
    def write(self, path, buf, offset, fh):
        """Write to a file.

        """
        length = len(bytearray(buf))
        fullPath = self._full_path(path)
        if (fullPath not in self.openFiles):
            return -1

        if (self.openFiles[fullPath] is None):
            self.openFiles[fullPath] = buf
            return length

        arr1 = bytearray(self.openFiles[fullPath][:offset])
        arr2 = bytearray(self.openFiles[fullPath][offset:])

        if len(arr2) > length:
            for i in range(0, length):
                arr2[i] = buf[i]
            arr1.append(arr2[:])
        else:
            arr1.append(bytearray(buf)[:])

        self.openFiles[fullPath] = bytes(arr1)
        return length
        

    @logged
    def truncate(self, path, length, fh=None):
        """Truncate a file.

        Truncate or extend the given file so that it is precisely size bytes
        long. See truncate(2) for details. This call is required for read/write
        filesystems, because recreating a file will first truncate it.

        """
        fullPath = self._full_path(path)

        if (fullPath not in self.openFiles):
            return -1
        
        arr = bytearray(self.openFiles[fullPath])

        if len(arr) < length:
            for i in range(len(arr), length):
                arr.append(0)
            self.openFiles[fullPath] = bytes(arr)
        elif length < len(arr):
            newArr = arr[:length]
            self.openFiles[fullPath] = bytes(newArr)
        
        return length

    #skip
    '''
    @logged
    def flush(self, path, fh):
        """Flush buffered information.

        Called on each close so that the filesystem has a chance to report
        delayed errors. Important: there may be more than one flush call for
        each open. Note: There is no guarantee that flush will ever be called
        at all!

        """
        return os.fsync(fh)
   '''                                            

    @logged
    def release(self, path, fh):
        """Release is called when FUSE is done with a file.

        This is the only FUSE function that doesn't have a directly
        corresponding system call, although close(2) is related. Release is
        called when FUSE is completely done with a file; at that point, you can
        free up any temporarily allocated data structures. The IBM document
        claims that there is exactly one release per open, but I don't know if
        that is true.

        """
        fullPath = self._full_path(path)

        if (fullPath not in self.openFiles or not os.path.exists(fullPath)):
            return -1
                    
        salt = os.urandom(16)
        kdf = PBKDF2HMAC(
                algorithm=hashes.SHA256(),
                length=32,
                salt=salt,
                iterations=480000,
            )
        key = base64.urlsafe_b64encode(kdf.derive(str.encode(self.password)))
        f = Fernet(key)
        encryptMsg = f.encrypt(self.openFiles[fullPath])
        file = os.open(fullPath, os.O_WRONLY)  
        file.write(salt)
        file.write(encryptMsg)
        file.close()

        del self.openFiles[fullPath]
        return 0
    
    #skip
'''    @logged
    def fsync(self, path, fdatasync, fh):
        """Flush any dirty information to disk.

        Flush any dirty information about the file to disk. If isdatasync is
        nonzero, only data, not metadata, needs to be flushed. When this call
        returns, all file data should be on stable storage. Many filesystems
        leave this call unimplemented, although technically that's a Bad Thing
        since it risks losing data. If you store your filesystem inside a plain
        file on another filesystem, you can implement this by calling fsync(2)
        on that file, which will flush too much data (slowing performance) but
        achieve the desired guarantee.

        """
        return self.flush(path, fh)
'''
if __name__ == '__main__':
    from sys import argv
    if len(argv) != 3:
        print('usage: %s <encrypted folder> <mountpoint>' % argv[0])
        exit(1)

    logging.basicConfig(level=logging.DEBUG)
    #create our virtual filesystem using argv[1] as the physical filesystem
    #and argv[2] as the virtual filesystem
    fuse = FUSE(EncFS(argv[1]), argv[2], foreground=True)
