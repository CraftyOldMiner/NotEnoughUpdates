public void onResourceManagerReloadOrig(net.minecraft.client.resources.IResourceManager resourceManager)
{
    net.minecraft.client.resources.model.ModelBakery modelbakery = new net.minecraft.client.resources.model.ModelBakery(resourceManager, this.texMap, this.modelProvider);
    this.modelRegistry = modelbakery.setupModelRegistry();
    this.defaultModel = (net.minecraft.client.resources.model.IBakedModel)this.modelRegistry.getObject(net.minecraft.client.resources.model.ModelBakery.MODEL_MISSING);
    this.modelProvider.reloadModels();
}
