package gnu.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class NativeResource {
	
	private final Logger log = Logger.getLogger(this.getClass());
	
	private boolean loaded = false;
	
	public synchronized void load(String libraryName) throws NativeResourceException {	
		if(loaded)
			return;
		loaded = true;
		if(System.getProperty(libraryName + ".userlib") != null) {
			try {
				if(System.getProperty(libraryName + ".userlib").equalsIgnoreCase("sys")) {
					System.loadLibrary(libraryName);
				} else {
					System.load(System.getProperty(libraryName + ".userlib"));
				}
				return;
			} catch (Exception ex){
				log.error("Unable to load native resource from given path.", ex);
				throw new NativeResourceException("Unable to load native resource from given path.\n" + ex.getLocalizedMessage());
			}
		}
		loadLib(libraryName);	
	}
	
	private void inJarLoad(String name)throws UnsatisfiedLinkError, NativeResourceException{
		//start by assuming the library can be loaded from the jar
		InputStream resourceSource = locateResource(name);
		File resourceLocation = prepResourceLocation(name);
		try {
			copyResource(resourceSource, resourceLocation);
		} catch (IOException e) {
			throw new UnsatisfiedLinkError();
		}
		loadResource(resourceLocation);
		testNativeCode();
	}
	private String[] armLibs = {"libNRJavaSerialv8_HF","libNRJavaSerialv8","libNRJavaSerialv7_HF","libNRJavaSerialv7","libNRJavaSerialv6_HF","libNRJavaSerialv6","libNRJavaSerialv5"};
	private void loadLib(String name) throws NativeResourceException {

		String libName = name.substring(name.indexOf("lib")+3);
		try {
			if(OSUtil.isARM()) {
				log.debug("Attempting arm variants");
				for(int i=0;i<armLibs.length;i++) {
					try {
						inJarLoad(armLibs[i]);
						log.debug("Arm lib success! "+armLibs[i]);
						return;
					}catch(UnsatisfiedLinkError e) {
						log.debug("Is not "+armLibs[i]);
					}
				}
			}else {
				inJarLoad(name);
			}
			return;
		} catch (UnsatisfiedLinkError ex) {
			if(OSUtil.isOSX() || OSUtil.isLinux()){
				try{
					inJarLoad("libNRJavaSerial_legacy");
					log.debug("Normal lib failed, using legacy..OK!");
					return;
				}catch(UnsatisfiedLinkError er){
					log.error("Unable to load native resource from given path.", er);
				}
			}else{
				log.error("Unable to load native resource from given path.", ex);
			}
			try{
				//check to see if the library is available in standard locations
				System.loadLibrary(libName);
				testNativeCode();
				return;
			}catch(UnsatisfiedLinkError e){
				try{
					name = "rxtxSerial";
					//last ditch effort to load
					System.loadLibrary( name);	
					testNativeCode();
					return;
				}catch(UnsatisfiedLinkError err){
					//System.err.println("Failed to load all possible JNI local and from: \n"+System.getProperty("java.library.path"));
					log.error("Unable to load native resource from given path.", err);
					throw new NativeResourceException("Unable to load deployed native resource");
				}
			}
			
		}
	}
	
	private void testNativeCode()throws UnsatisfiedLinkError {
		CommPortIdentifier.getPortIdentifiers();
	}

	private InputStream locateResource(String name) {
		name += OSUtil.getExtension();
		String file="";
		if(OSUtil.isOSX()) {
			file="/native/osx/" + name;
		}else if(OSUtil.isWindows()) {
			if(OSUtil.is64Bit()){
				file="/native/windows/x86_64/" + name;
			}else {
				file="/native/windows/x86_32/" + name;
			}
		}else if(OSUtil.isLinux()) {
			if(OSUtil.isARM()) {
				file = "/native/linux/ARM/" + name;
			}else if(OSUtil.isPPC()) {
				file = "/native/linux/PPC/" + name;
			}else {
				if(OSUtil.is64Bit()) {
					file="/native/linux/x86_64/" + name;
				}else {
					file="/native/linux/x86_32/" + name;
				}
			}
		}else if(OSUtil.isFreeBSD()) {
			if(OSUtil.is64Bit()) {
				file="/native/freebsd/x86_64/" + name;
			}else {
				file="/native/freebsd/x86_32/" + name;
			}
		}else{
			log.debug("Can't load native file: "+name+" for os arch: "+OSUtil.getOsArch());
			return null;
		}
		log.debug("Loading "+file);
		return getClass().getResourceAsStream(file);
	}
	
	private void loadResource(File resource) {
		if(!resource.canRead())
			throw new RuntimeException("Cant open JNI file: "+resource.getAbsolutePath());
		log.debug("Loading: "+resource.getAbsolutePath());
		try {
			System.load(resource.getAbsolutePath());
		} catch(UnsatisfiedLinkError e){
			log.error(e.getMessage());
			throw e;
		}
	}

	private void copyResource(InputStream io, File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		
		
		byte[] buf = new byte[256];
		int read = 0;
		while ((read = io.read(buf)) > 0) {
			fos.write(buf, 0, read);
		}
		fos.close();
		io.close();
	}

	private File prepResourceLocation(String fileName) throws NativeResourceException {		
		String tmpDir = System.getProperty("java.io.tmpdir");
		//String tmpDir = "M:\\";
		if ((tmpDir == null) || (tmpDir.length() == 0)) {
			tmpDir = "tmp";
		}
		
		String displayName = new File(fileName).getName().split("\\.")[0];
		
		String user = System.getProperty("user.name");
		
		File fd = null;
		File dir = null;
		
		for(int i = 0; i < 10; i++) {
			dir = new File(tmpDir, displayName + "_" + user + "_" + (i));
			if (dir.exists()) {
				if (!dir.isDirectory()) {
					continue;
				}
				
				try {
					File[] files = dir.listFiles();
					for (int j = 0; j < files.length; j++) {
						if (!files[j].delete()) {
							continue;
						}
					}
				} catch (Throwable e) {
					
				}
			}
			
			if ((!dir.exists()) && (!dir.mkdirs())) {
				continue;
			}
			
			try {
				dir.deleteOnExit();
			} catch (Throwable e) {
				// Java 1.1 or J9
			}
			
			fd = new File(dir, fileName + OSUtil.getExtension());
			if ((fd.exists()) && (!fd.delete())) {
				continue;
			}
			
			try {
				if (!fd.createNewFile()) {
					continue;
				}
			} catch (IOException e) {
				continue;
			} catch (Throwable e) {
				// Java 1.1 or J9
			}
			
			break;
		}
		
		if(fd == null || !fd.canRead()) {
			throw new NativeResourceException("Unable to deploy native resource");
		}
		log.debug("Local file: "+fd.getAbsolutePath());
		return fd;
	}
	
	private static class OSUtil {
		
		private static final Logger staticLog = Logger.getLogger(OSUtil.class);
		
		public static boolean is64Bit() {
			staticLog.debug("Arch: "+getOsArch());
			return getOsArch().startsWith("x86_64") || getOsArch().startsWith("amd64");
		}
		public static boolean isARM() {
			return getOsArch().startsWith("arm");
		}
		public static boolean isPPC() {
			return getOsArch().toLowerCase().contains("ppc");
		}
		public static boolean isCortexA8(){
			if(isARM()){
				//TODO check for cortex a8 vs arm9 generic
				return true;
			}
			return false;
		}
		public static boolean isWindows() {
			staticLog.debug("OS name: "+getOsName());
			return getOsName().toLowerCase().startsWith("windows") ||getOsName().toLowerCase().startsWith("microsoft") || getOsName().toLowerCase().startsWith("ms");
		}
		
		public static boolean isLinux() {
			return getOsName().toLowerCase().startsWith("linux");
		}

		public static boolean isFreeBSD() {
			return getOsName().toLowerCase().startsWith("freebsd");
		}

		public static boolean isOSX() {
			return getOsName().toLowerCase().startsWith("mac");
		}
		
		public static String getExtension() {
			if(isWindows()) {
				return ".dll";
			}
			
			if(isLinux() || isFreeBSD()) {
				return ".so";
			}
			
			if(isOSX()) {
				return ".jnilib";
			}
			
			return "";
		}
		
		public static String getOsName() {	
			return System.getProperty("os.name");
		}
		
		public static String getOsArch() {
			return System.getProperty("os.arch");
		}
		
		@SuppressWarnings("unused")
		public static String getIdentifier() {
			return getOsName() + " : " + getOsArch();
		}
	}
}
