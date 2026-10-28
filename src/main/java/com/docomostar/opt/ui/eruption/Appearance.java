package com.docomostar.opt.ui.eruption;

import java.util.ArrayList;
import java.util.List;

public final class Appearance extends Object3D {
    public static final int MCE_MATERIAL_TYPE_AMBIENT=0, MCE_MATERIAL_TYPE_DIFFUSE=1,
            MCE_MATERIAL_TYPE_EMISSIVE=2, MCE_MATERIAL_TYPE_SPECULAR=3;
    public static final int MCE_BLEND_MODE_ALPHA=0, MCE_BLEND_MODE_ALPHA_ADD=1,
            MCE_BLEND_MODE_MODULATE=2, MCE_BLEND_MODE_MODULATE_X2=3, MCE_BLEND_MODE_REPLACE=4;
    public static final int MCE_CULLING_MODE_CULL_BACK=0, MCE_CULLING_MODE_CULL_FRONT=1, MCE_CULLING_MODE_CULL_NONE=2;
    public static final int MCE_LIGHTING=1, MCE_ALPHA_WRITE=2, MCE_COLOR_WRITE=4, MCE_DEPTH_TEST=8,
            MCE_DEPTH_WRITE=16, MCE_PERSPECTIVE_CORRECT=32, MCE_VERTEX_COLOR=64, MCE_VISIBLE=1024;
    public static final int MCE_TEXTURE_FILTER_TYPE_MIN=0, MCE_TEXTURE_FILTER_TYPE_MAG=1,
            MCE_TEXTURE_FILTER_NEAREST=0, MCE_TEXTURE_FILTER_LINEAR=1;
    int properties=1084, blendMode=0, cullingMode=0, alphaThreshold=0;
    final int[] colors={0xff000000,0xffffffff,0xff000000,0xff000000};
    final Texture[] textures = new Texture[2];
    final int[] textureIds = {-1,-1};
    final int[][] filters = {{0,0},{0,0}};
    final int[] textureBlend = {4,4};
    public Appearance() { super(MCE_CLASS_TYPE_APPEARANCE); }
    public void setProperties(int value) { requireLive(); properties=value; }
    public int getProperties() { return properties; }
    public void setBlendMode(int value) {
        requireLive(); if (value<0 || value>4) throw new IllegalArgumentException("Blend mode"); blendMode=value;
    }
    public void setCullingMode(int value) {
        requireLive(); if (value<0 || value>2) throw new IllegalArgumentException("Culling mode"); cullingMode=value;
    }
    public int getCullingMode() { return cullingMode; }
    public void setAlphaThreshold(int value) { alphaThreshold=value; }
    public int getAlphaThreshold() { return alphaThreshold; }
    public void setTexture(int unit, Texture texture) { requireLive(); textures[unit]=texture; }
    public Texture getTexture(int unit) { return textures[unit]; }
    public void setTextureRefGid(int unit, int gid) { textureIds[unit]=gid; }
    public int getTextureRefGid(int unit) { return textureIds[unit]; }
    public void setTextureFilter(int unit, int type, int filter) { requireLive(); filters[unit][type]=filter; }
    public int getTextureFilter(int unit, int type) { return filters[unit][type]; }
    public void setTextureBlendMode(int unit, int mode) { textureBlend[unit]=mode; }
    public int getTextureBlendMode(int unit) { return textureBlend[unit]; }
    public void setColor(int material, int color) { requireLive();colors[material]=color; }
    public int getColor(int material) { requireLive();return colors[material]; }
    @Override List<? extends Object3D> references() {
        List<Object3D> result=new ArrayList<>();
        for (Texture t:textures) if(t!=null) result.add(t);
        return result;
    }
}
