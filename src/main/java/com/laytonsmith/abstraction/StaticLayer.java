/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.abstraction;

import com.laytonsmith.PureUtilities.ClassDiscovery;
import com.sk89q.commandhelper.CommandHelperPlugin;


/**
 * Unfortunately some methods just can't be overridden.
 * @author layton
 */
public class StaticLayer {
    
    private static Convertor convertor = null;
    static{
        InitConvertor();
    }
    
    private static void InitConvertor(){
        Class[] classes = ClassDiscovery.GetClassesWithAnnotation(convert.class);
        for(Class c : classes){
            Class[] implemented = c.getInterfaces();
            boolean doesImplement = false;
            for(Class inter : implemented){
                if(inter.equals(Convertor.class)){                    
                    doesImplement = true;         
                    break;
                }
            }
            if(!doesImplement){
                System.out.println("The Convertor " + c.getSimpleName() + " doesn't implement Convertor!");
            }
            convert convert = (convert)c.getAnnotation(convert.class);
            if(convert.type() == Implementation.GetServerType()){
                //This is what we're looking for, instatiate it.
                try{
                    if(convertor != null){
                        //Uh... There are more than one implementations for this server type
                        System.out.println("More than one Convertor for this server type was detected!");
                    }
                    convertor = (Convertor) c.newInstance();                    
                    //At this point we are all set
                } catch(Exception e){
                    System.out.println("Tried to instantiate the Convertor, but couldn't!");
                }
            }
        }
        if(convertor == null){
            System.out.println("Could not find a suitable convertor! You will experience serious issues with this plugin.");
        }
    }

    public static MCLocation GetLocation(MCWorld w, double x, double y, double z, float yaw, float pitch) {
        return convertor.GetLocation(w, x, y, z, yaw, pitch);
    }
    
    public static Class GetServerEventMixin() {
        return convertor.GetServerEventMixin();
    }
    
    public static MCLocation GetLocation(MCWorld w, double x, double y, double z){
        return GetLocation(w, x, y, z, 0, 0);
    }

    public static MCItemStack GetItemStack(int type, int qty) {
        return convertor.GetItemStack(type, qty);
    }
    
    public static MCServer GetServer(){
        return convertor.GetServer();
    }
    
    public static MCEnchantment GetEnchantmentByName(String name){
        return convertor.GetEnchantmentByName(name);
    }
    
    public static MCEnchantment[] GetEnchantmentValues(){
        return convertor.GetEnchantmentValues();
    }

    public static void Startup(CommandHelperPlugin chp) {
        convertor.Startup(chp);
    }
    
    /**
     * Returns the data value of the specified material name, or -1 if none was found.
     * @param materialName
     * @return 
     */
    public static int LookupItemId(String materialName){
        return convertor.LookupItemId(materialName);
    }
    
    /**
     * Returns the name of the material, given the material's ID.
     * @param id
     * @return 
     */
    public static String LookupMaterialName(int id){
        return convertor.LookupMaterialName(id);
    }
    
}