    /**
     * Starts the game: initializes the canvas, the title, the settings, etcetera.
     */
    private void startGameTest() throws org.lwjgl.LWJGLException, java.io.IOException
    {
        this.gameSettings = new net.minecraft.client.settings.GameSettings(this, this.mcDataDir);
        this.defaultResourcePacks.add(this.mcDefaultResourcePack);
        this.setWindowIcon();
        this.setInitialDisplayMode();
        this.createDisplay();
        net.minecraft.client.renderer.OpenGlHelper.initializeTextures();
        this.framebufferMc = new net.minecraft.client.shader.Framebuffer(this.displayWidth, this.displayHeight, true);
        this.framebufferMc.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.registerMetadataSerializers();
        this.mcResourcePackRepository = new net.minecraft.client.resources.ResourcePackRepository(this.fileResourcepacks, new java.io.File(this.mcDataDir, "server-resource-packs"), this.mcDefaultResourcePack, this.metadataSerializer_, this.gameSettings);
        this.mcResourceManager = new net.minecraft.client.resources.SimpleReloadableResourceManager(this.metadataSerializer_);
        this.mcLanguageManager = new net.minecraft.client.resources.LanguageManager(this.metadataSerializer_, this.gameSettings.language);
        this.mcResourceManager.registerReloadListener(this.mcLanguageManager);
        refreshResources();
        // NOTE: The above two lines replace: net.minecraftforge.fml.client.FMLClientHandler.instance().beginMinecraftLoading(this, this.defaultResourcePacks, this.mcResourceManager);
        this.renderEngine = new net.minecraft.client.renderer.texture.TextureManager(this.mcResourceManager);
        this.mcResourceManager.registerReloadListener(this.renderEngine);
        // TODO: replace: net.minecraftforge.fml.client.SplashProgress.drawVanillaScreen(this.renderEngine);
        this.initStream();
        this.skinManager = new net.minecraft.client.resources.SkinManager(this.renderEngine, new java.io.File(this.fileAssets, "skins"), this.sessionService);
        this.saveLoader = new net.minecraft.world.chunk.storage.AnvilSaveConverter(new java.io.File(this.mcDataDir, "saves"));
        this.mcSoundHandler = new net.minecraft.client.audio.SoundHandler(this.mcResourceManager, this.gameSettings);
        this.mcResourceManager.registerReloadListener(this.mcSoundHandler);
        this.mcMusicTicker = new net.minecraft.client.audio.MusicTicker(this);
        this.fontRendererObj = new net.minecraft.client.gui.FontRenderer(this.gameSettings, new net.minecraft.util.ResourceLocation("textures/font/ascii.png"), this.renderEngine, false);

        if (this.gameSettings.language != null)
        {
            this.fontRendererObj.setUnicodeFlag(this.isUnicode());
            this.fontRendererObj.setBidiFlag(this.mcLanguageManager.isCurrentLanguageBidirectional());
        }

        this.standardGalacticFontRenderer = new net.minecraft.client.gui.FontRenderer(this.gameSettings, new net.minecraft.util.ResourceLocation("textures/font/ascii_sga.png"), this.renderEngine, false);
        this.mcResourceManager.registerReloadListener(this.fontRendererObj);
        this.mcResourceManager.registerReloadListener(this.standardGalacticFontRenderer);
        this.mcResourceManager.registerReloadListener(new net.minecraft.client.resources.GrassColorReloadListener());
        this.mcResourceManager.registerReloadListener(new net.minecraft.client.resources.FoliageColorReloadListener());
        // NOTE: javassist complains about compiling this part and it doesn't seem to break anything major to have it commented out
        // net.minecraft.stats.AchievementList.openInventory.setStatStringFormatter(new net.minecraft.stats.IStatStringFormat()
        // {
        //     /**
        //      * Formats the strings based on 'IStatStringFormat' interface.
        //      *  
        //      * @param str The String to format
        //      */
        //     public java.lang.String formatString(java.lang.String str)
        //     {
        //         try
        //         {
        //             return java.lang.String.format(str, new java.lang.Object[] {GameSettings.getKeyDisplayString(Minecraft.this.gameSettings.keyBindInventory.getKeyCode())});
        //         }
        //         catch (java.lang.Exception exception)
        //         {
        //             return "Error: " + exception.getLocalizedMessage();
        //         }
        //     }
        // });
        this.mouseHelper = new net.minecraft.util.MouseHelper();
        net.minecraftforge.fml.common.ProgressManager.ProgressBar bar= net.minecraftforge.fml.common.ProgressManager.push("Rendering Setup", 5, true);
        bar.step("GL Setup");
        this.checkGLError("Pre startup");
        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.shadeModel(7425);
        net.minecraft.client.renderer.GlStateManager.clearDepth(1.0D);
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        net.minecraft.client.renderer.GlStateManager.depthFunc(515);
        net.minecraft.client.renderer.GlStateManager.enableAlpha();
        net.minecraft.client.renderer.GlStateManager.alphaFunc(516, 0.1F);
        net.minecraft.client.renderer.GlStateManager.cullFace(1029);
        net.minecraft.client.renderer.GlStateManager.matrixMode(5889);
        net.minecraft.client.renderer.GlStateManager.loadIdentity();
        net.minecraft.client.renderer.GlStateManager.matrixMode(5888);
        this.checkGLError("Startup");
        // bar.step("Loading Texture Map");
        this.textureMapBlocks = new net.minecraft.client.renderer.texture.TextureMap("textures",true);
        this.textureMapBlocks.setMipmapLevels(this.gameSettings.mipmapLevels);
        this.renderEngine.loadTickableTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture, this.textureMapBlocks);
        this.renderEngine.bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);
        this.textureMapBlocks.setBlurMipmapDirect(false, this.gameSettings.mipmapLevels > 0);
        // bar.step("Loading Model Manager");
        this.modelManager = new net.minecraft.client.resources.model.ModelManager(this.textureMapBlocks);
        this.mcResourceManager.registerReloadListener(this.modelManager);
        // bar.step("Loading Item Renderer");
        this.renderItem = new net.minecraft.client.renderer.entity.RenderItem(this.renderEngine, this.modelManager);
        this.renderManager = new net.minecraft.client.renderer.entity.RenderManager(this.renderEngine, this.renderItem);
        this.itemRenderer = new net.minecraft.client.renderer.ItemRenderer(this);
        this.mcResourceManager.registerReloadListener(this.renderItem);
        // bar.step("Loading Entity Renderer");
        this.entityRenderer = new net.minecraft.client.renderer.EntityRenderer(this, this.mcResourceManager);
        this.mcResourceManager.registerReloadListener(this.entityRenderer);
        this.blockRenderDispatcher = new net.minecraft.client.renderer.BlockRendererDispatcher(this.modelManager.getBlockModelShapes(), this.gameSettings);
        this.mcResourceManager.registerReloadListener(this.blockRenderDispatcher);
        this.renderGlobal = new net.minecraft.client.renderer.RenderGlobal(this);
        this.mcResourceManager.registerReloadListener(this.renderGlobal);
        this.guiAchievement = new net.minecraft.client.gui.achievement.GuiAchievement(this);
        net.minecraft.client.renderer.GlStateManager.viewport(0, 0, this.displayWidth, this.displayHeight);
        this.effectRenderer = new net.minecraft.client.particle.EffectRenderer(this.theWorld, this.renderEngine);
        this.checkGLError("Post startup");
        this.ingameGUI = new net.minecraft.client.gui.GuiIngame(this);
        this.renderGlobal.makeEntityOutlineShader();

        this.gameSettings.pauseOnLostFocus = false;
        //this.displayGuiScreen(new net.minecraft.client.gui.GuiMainMenu());
        this.displayGuiScreen(new io.github.moulberry.notenoughupdates.dungeons.GuiDungeonMapEditor());
        
        this.mojangLogo = null;
        this.loadingScreen = new net.minecraft.client.LoadingScreenRenderer(this);
        this.renderGlobal.makeEntityOutlineShader();

        net.minecraft.client.resources.FolderResourcePack folderResourcePack =
            new net.minecraft.client.resources.FolderResourcePack(new java.io.File("/home/dave/git/NotEnoughUpdates/build/classes/java/main/"));

        ((net.minecraft.client.resources.SimpleReloadableResourceManager)(this.mcResourceManager)).reloadResourcePack(folderResourcePack);
    }

