/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.storage.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.presentation.util.PropertyReader;

/**
 * Mehtods to help wotk with images
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MediaUtils
{
    private static Logger logger = Logger.getLogger(MediaUtils.class);

    /**
     * Return true if imagemagick is installed on the current system<br/>
     * TODO Ye: Execute when upload page shows and show install ImageMagick tips
     * 
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static boolean verifyImageMagickInstallation() throws IOException, URISyntaxException
    {
        // TODO set in properties
        String imPath = getImageMagickInstallationPath();
        ConvertCmd cmd = new ConvertCmd(false);
        cmd.setSearchPath(imPath);
        IMOperation op = new IMOperation();
        // get ImageMagick version
        op.version();
        try
        {
            cmd.run(op);
        }
        catch (Exception e)
        {
            logger.debug("imagemagick not installed");
            return false;
        }
        return true;
    }

    /**
     * User imagemagick to convert any image into a jpeg
     * 
     * @param bytes
     * @param extension
     * @throws IOException
     * @throws URISyntaxException
     * @throws InterruptedException
     * @throws IM4JavaException
     */
    public static byte[] convertToJPEG(File tmp, String extension) throws IOException, URISyntaxException,
            InterruptedException, IM4JavaException
    {
        String path = tmp.getAbsolutePath() + "[1]";
        String magickPath = getImageMagickInstallationPath();
        // TODO Ye:ConvertCmd(true) to use GraphicsMagick, which is said faster
        ConvertCmd cmd = new ConvertCmd(false);
        cmd.setSearchPath(magickPath);
        // create the operation, add images and operators/options
        IMOperation op = new IMOperation();
        // if (StorageUtils.getMimeType(extension).contains("video") || true)
        // path = path + "[1]";
        op.addImage(path);
        File jpeg = File.createTempFile("uploadMagick" + IdentifierUtil.newRandomId(), ".jpg");
        try
        {
            op.addImage(jpeg.getAbsolutePath());
            cmd.run(op);
            return FileUtils.readFileToByteArray(jpeg);
        }
        finally
        {
            FileUtils.deleteQuietly(jpeg);
        }
    }

    private int getNonBlankFrame(String path)
    {
        // convert scanned_page.pdf -shave 1%x1% -resize 40% -fuzz 10% -trim +repage info: | grep ' 1x1 '
        return 0;
    }

    /**
     * Return property imeji.imagemagick.installpath
     * 
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private static String getImageMagickInstallationPath() throws IOException, URISyntaxException
    {
        return PropertyReader.getProperty("imeji.imagemagick.installpath");
    }
}