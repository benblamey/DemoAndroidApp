package com.ml4d.ohow;

import java.io.File;
import java.io.IOException;
import com.ml4d.ohow.exceptions.ExternalStorageException;
import android.content.res.Resources;
import android.os.Environment;

/**
 * Various utility methods relating to external storage.
 * @author ben
 */
public class ExternalStorageUtilities {

	/**
	 * Creates an empty temporary file in the external storage area.
	 * 
	 * Note the file is not automatically marked for deletion. Callers may wish to use File.deleteOnExit() if appropriate.
	 * @param suffix The file extension.
	 * @param resources
	 * @return An java.io.File instance representing the newly created file.
	 * @throws IOException
	 */
	public static File getTempFileOnExternalStorage(String suffix, Resources resources) throws IOException {

		String extStorageState = Environment.getExternalStorageState();

		if (Environment.MEDIA_BAD_REMOVAL.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_BAD_REMOVAL));
		} else if (Environment.MEDIA_CHECKING.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_CHECKING));
		} else if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
			// The external card is OK - create a temporary file.
			return File.createTempFile("OHOW", suffix, android.os.Environment.getExternalStorageDirectory());
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_MOUNTED_READ_ONLY));
		} else if (Environment.MEDIA_NOFS.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_NOFS));
		} else if (Environment.MEDIA_REMOVED.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_REMOVED));
		} else if (Environment.MEDIA_SHARED.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_SHARED));
		} else if (Environment.MEDIA_UNMOUNTABLE.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_UNMOUNTABLE));
		} else if (Environment.MEDIA_UNMOUNTED.equals(extStorageState)) {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_MEDIA_UNMOUNTED));
		} else {
			throw new ExternalStorageException(resources.getString(R.string.external_storage_UNKNOWN_STATE));
		}
	}
}
