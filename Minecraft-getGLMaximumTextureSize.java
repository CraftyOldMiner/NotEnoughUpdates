public static int getGLMaximumTextureSizeOrig()
{
	for (int i = 0x4000; i > 0; i >>= 1)
        {
            org.lwjgl.opengl.GL11.glTexImage2D(org.lwjgl.opengl.GL11.GL_PROXY_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_RGBA, i, i, 0, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer)null);
            if (org.lwjgl.opengl.GL11.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_PROXY_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH) != 0)
            {
                return i;
            }
        }
        return -1;
}
