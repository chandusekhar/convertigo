    /**
     * Function RootPageAction
     *   
     * @param page  , the current page
     * @param props , the object which holds properties key-value pairs
     * @param vars  , the object which holds variables key-value pairs
     */
    RootPageAction(page: C8oPageBase, props, vars) : Promise<any> {
        return new Promise((resolve, reject) => {
            let q:string = props.page; // qname of page
            let p:string = q.substring(q.lastIndexOf('.')+1);
            let version:string = props.tplVersion ? props.tplVersion : '';
            let v:any = version.localeCompare("7.7.0.2") >= 0 ? p : page.getPageByName(p);
            
            page.routerProvider.setRoot(v, props.data, {
                animate: props.animate == "true" ? true:false,
                duration: props.animate_duration
            })
            .then((res:any) => {
                resolve(res)
            }).catch((error:any) => {
                reject(error)
            })
        });
    }