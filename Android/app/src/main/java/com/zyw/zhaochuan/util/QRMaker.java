package com.zyw.zhaochuan.util;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRMaker
{
	//private ImageView sweepIV;
	private static int QR_WIDTH = 200;
	private static int	QR_HEIGHT = 200;

	/**
	 * 根据内容生成二维码
	 * @param content
	 * @return 返回空生成失败
     */
	public static Bitmap createQRImage(String content)
	{
		Bitmap bitmap=null;
		try
		{
			if (content == null || "".equals(content) || content.length() < 1)
			{
				return null;
			}
			Map<EncodeHintType, String> hints = new HashMap<EncodeHintType, String>();
			hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
			BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);
			int[] pixels = new int[QR_WIDTH * QR_HEIGHT];
			for (int y = 0; y < QR_HEIGHT; y++)
			{
				for (int x = 0; x < QR_WIDTH; x++)
				{
					if (bitMatrix.get(x, y))
					{
						pixels[y * QR_WIDTH + x] = 0xff000000;
					}
					else
					{
						pixels[y * QR_WIDTH + x] = 0xffffffff;
					}
				}
			}
			 bitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, QR_WIDTH, 0, 0, QR_WIDTH, QR_HEIGHT);
		}
		catch (WriterException e)
		{
			e.printStackTrace();
		}
		return bitmap;
	}
}
